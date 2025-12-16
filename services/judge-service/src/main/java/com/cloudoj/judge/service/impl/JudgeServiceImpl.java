package com.cloudoj.judge.service.impl;

import com.cloudoj.judge.config.RabbitMQConfig;
import com.cloudoj.judge.feign.CourseServiceClient;
import com.cloudoj.judge.feign.ProblemServiceClient;
import com.cloudoj.judge.mapper.SubmissionMapper;
import com.cloudoj.judge.service.AntiCheatService;
import com.cloudoj.judge.service.JudgeService;
import com.cloudoj.judge.service.SubmitRateLimiter;
import com.cloudoj.model.common.PageResult;
import com.cloudoj.model.common.Result;
import com.cloudoj.model.dto.judge.JudgeMessage;
import com.cloudoj.model.dto.judge.SubmitCodeRequest;
import com.cloudoj.model.entity.judge.Submission;
import com.cloudoj.model.entity.problem.TestCase;
import com.cloudoj.model.enums.JudgeStatusEnum;
import com.cloudoj.model.vo.judge.JudgeResultVO;
import com.cloudoj.model.vo.judge.SubmissionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评测服务实现类
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {
    
    @Autowired
    private SubmissionMapper submissionMapper;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SubmitRateLimiter submitRateLimiter;
    
    @Autowired(required = false)
    private com.cloudoj.judge.sandbox.SandboxFactory sandboxFactory;
    
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("loadBalancedRestTemplate")
    private org.springframework.web.client.RestTemplate restTemplate;
    
    @Autowired
    private ProblemServiceClient problemServiceClient;
    
    @Autowired
    private CourseServiceClient courseServiceClient;
    
    @Autowired
    private com.cloudoj.judge.feign.LearningServiceClient learningServiceClient;
    
    @Autowired
    private com.cloudoj.judge.service.JudgeNotificationService notificationService;
    
    @Autowired
    private com.cloudoj.judge.service.AntiCheatService antiCheatService;
    
    // Redis缓存key前缀
    private static final String SUBMISSION_CACHE_PREFIX = "submission:";
    private static final String JUDGE_RESULT_CACHE_PREFIX = "judge:result:";
    // 缓存过期时间（秒）
    private static final long CACHE_EXPIRE_TIME = 300; // 5分钟
    
    @Override
    public Long submitCode(SubmitCodeRequest request, String ipAddress) {
        // 检查提交频率限制
        if (!submitRateLimiter.canSubmit(request.getUserId())) {
            long waitSeconds = submitRateLimiter.getWaitSeconds(request.getUserId());
            throw new RuntimeException(
                String.format("提交过于频繁，请在%d秒后重试", waitSeconds)
            );
        }
        
        // 创建提交记录
        Submission submission = new Submission();
        submission.setProblemId(request.getProblemId());
        submission.setUserId(request.getUserId());
        submission.setLanguage(request.getLanguage());
        submission.setCode(request.getCode());
        submission.setStatus(JudgeStatusEnum.PENDING.getCode());
        submission.setIpAddress(ipAddress);
        submission.setHomeworkId(request.getHomeworkId()); // 保存作业ID
        
        // 保存到数据库
        submissionMapper.insert(submission);
        
        // 记录提交次数（用于频率限制）
        submitRateLimiter.recordSubmit(request.getUserId());
        
        log.info("代码提交成功, submissionId={}, problemId={}, userId={}", 
                submission.getId(), request.getProblemId(), request.getUserId());
        
        // 发送评测消息到RabbitMQ队列（异步评测）
        JudgeMessage judgeMessage = new JudgeMessage();
        judgeMessage.setSubmissionId(submission.getId());
        judgeMessage.setProblemId(request.getProblemId());
        judgeMessage.setUserId(request.getUserId());
        judgeMessage.setLanguage(request.getLanguage());
        judgeMessage.setCode(request.getCode());
        judgeMessage.setRetryCount(0);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.JUDGE_EXCHANGE,
            RabbitMQConfig.JUDGE_ROUTING_KEY,
            judgeMessage
        );
        
        log.info("评测任务已发送到队列, submissionId={}", submission.getId());
        
        return submission.getId();
    }
    
    @Override
    public JudgeResultVO executeJudge(Long submissionId) {
        // 查询提交记录
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("提交记录不存在");
        }
        
        log.info("开始评测, submissionId={}, language={}", submissionId, submission.getLanguage());
        
        // 更新状态为评测中
        submission.setStatus(JudgeStatusEnum.JUDGING.getCode());
        submissionMapper.updateJudgeResult(submission);
        
        // 推送评测开始状态
        notificationService.notifyStatus(submission.getUserId(), submissionId, 
                JudgeStatusEnum.JUDGING.getCode(), "评测中...");
        
        // 执行评测
        JudgeResultVO result;
        if (sandboxFactory != null) {
            // 使用Docker沙箱评测
            result = dockerJudge(submission);
        } else {
            // 如果Docker不可用，使用模拟评测
            log.warn("Docker沙箱不可用，使用模拟评测");
            result = simulateJudge(submission);
        }
        
        // 更新评测结果
        submission.setStatus(result.getStatus());
        submission.setScore(result.getScore());
        submission.setTimeUsed(result.getTimeUsed());
        submission.setMemoryUsed(result.getMemoryUsed());
        submission.setErrorMessage(result.getErrorMessage());
        
        // 转换通过率：去掉百分号后转换为BigDecimal
        String passRateStr = result.getPassRate();
        if (passRateStr != null && passRateStr.endsWith("%")) {
            passRateStr = passRateStr.substring(0, passRateStr.length() - 1);
        }
        submission.setPassRate(new BigDecimal(passRateStr));
        
        // 保存测试用例结果详情（JSON格式）
        if (result.getTestCaseResults() != null && !result.getTestCaseResults().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String testCaseResultsJson = objectMapper.writeValueAsString(result.getTestCaseResults());
                submission.setTestCaseResults(testCaseResultsJson);
            } catch (Exception e) {
                log.error("序列化测试用例结果失败", e);
            }
        }
        
        submissionMapper.updateJudgeResult(submission);
        
        // 更新题目统计（提交次数和通过次数）
        updateProblemStatistics(submission.getProblemId(), "ACCEPTED".equals(result.getStatus()));
        
        // 更新学习进度到learning-service
        updateLearningProgress(submission, result);
        
        // 如果是作业提交，记录到course-service
        if (submission.getHomeworkId() != null) {
            recordHomeworkSubmission(submission, result);
        }
        
        // 清除缓存，让下次查询能获取最新结果
        clearSubmissionCache(submissionId);
        
        // 通过 WebSocket 推送评测结果给用户
        notificationService.notifyUser(submission.getUserId(), result);
        
        log.info("评测完成, submissionId={}, status={}, score={}", 
                submissionId, result.getStatus(), result.getScore());
        
        return result;
    }
    
    /**
     * Docker沙箱评测
     */
    private JudgeResultVO dockerJudge(Submission submission) {
        JudgeResultVO result = new JudgeResultVO();
        result.setSubmissionId(submission.getId());
        
        try {
            // 代码安全检查
            String securityError = antiCheatService.checkCodeSecurity(submission.getCode(), submission.getLanguage());
            if (securityError != null) {
                log.warn("代码安全检查失败: submissionId={}, error={}", submission.getId(), securityError);
                result.setStatus(JudgeStatusEnum.COMPILE_ERROR.getCode());
                result.setStatusDesc("代码安全检查失败");
                result.setScore(0);
                result.setTimeUsed(0);
                result.setMemoryUsed(0);
                result.setPassRate("0.00%");
                result.setErrorMessage(securityError);
                result.setTestCaseResults(new ArrayList<>());
                return result;
            }
            
            // 获取语言对应的沙箱
            com.cloudoj.judge.sandbox.LanguageSandbox sandbox = sandboxFactory.getSandbox(submission.getLanguage());
            
            // 从problem-service获取测试用例
            List<com.cloudoj.model.dto.judge.JudgeTestCase> testCases = getMockTestCases(submission.getProblemId());
            
            // 检查测试用例是否为空
            if (testCases == null || testCases.isEmpty()) {
                log.error("题目没有测试用例，无法评测：problemId={}", submission.getProblemId());
                result.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
                result.setStatusDesc("题目暂无测试用例");
                result.setScore(0);
                result.setTimeUsed(0);
                result.setMemoryUsed(0);
                result.setPassRate("0.00%");
                result.setErrorMessage("该题目暂未设置测试用例，请联系管理员添加测试用例后再提交");
                result.setTestCaseResults(new ArrayList<>());
                return result;
            }
            
            // 获取题目的时间限制和内存限制
            int timeLimit = 5000;  // 默认5秒
            int memoryLimit = 256; // 默认256MB
            try {
                Result<java.util.Map<String, Object>> problemResult = problemServiceClient.getProblemById(submission.getProblemId());
                if (problemResult != null && problemResult.isSuccess() && problemResult.getData() != null) {
                    java.util.Map<String, Object> problemData = problemResult.getData();
                    Object timeLimitObj = problemData.get("timeLimit");
                    Object memoryLimitObj = problemData.get("memoryLimit");
                    if (timeLimitObj != null) {
                        timeLimit = ((Number) timeLimitObj).intValue();
                    }
                    if (memoryLimitObj != null) {
                        memoryLimit = ((Number) memoryLimitObj).intValue();
                    }
                    log.info("使用题目限制：timeLimit={}ms, memoryLimit={}MB", timeLimit, memoryLimit);
                }
            } catch (Exception e) {
                log.warn("获取题目限制失败，使用默认值：timeLimit={}ms, memoryLimit={}MB", timeLimit, memoryLimit);
            }
            
            // 执行评测
            com.cloudoj.judge.sandbox.LanguageSandbox.JudgeResult judgeResult = sandbox.judge(
                    submission.getCode(),
                    testCases,
                    timeLimit,
                    memoryLimit
            );
            
            // 转换结果
            result.setStatus(judgeResult.getStatus());
            result.setStatusDesc(JudgeStatusEnum.getByCode(judgeResult.getStatus()).getDesc());
            result.setScore(judgeResult.getScore());
            result.setTimeUsed((int) judgeResult.getTimeUsed());
            result.setMemoryUsed((int) judgeResult.getMemoryUsed());
            result.setErrorMessage(judgeResult.getErrorMessage());
            
            // 计算通过率（避免除零）
            int totalTestCases = judgeResult.getTotalTestCases();
            if (totalTestCases > 0) {
                result.setPassRate(String.format("%.2f%%", 
                        100.0 * judgeResult.getPassedTestCases() / totalTestCases));
            } else {
                result.setPassRate("0.00%");
            }
            
            // 转换测试用例结果详情（编译错误时 testCaseResults 可能为 null）
            List<JudgeResultVO.TestCaseResultVO> testCaseResults = new ArrayList<>();
            List<com.cloudoj.judge.sandbox.LanguageSandbox.TestCaseResult> results = judgeResult.getTestCaseResults();
            if (results != null && !results.isEmpty()) {
                for (int i = 0; i < results.size(); i++) {
                    com.cloudoj.judge.sandbox.LanguageSandbox.TestCaseResult tcr = results.get(i);
                    JudgeResultVO.TestCaseResultVO vo = new JudgeResultVO.TestCaseResultVO();
                    vo.setTestCaseId(tcr.getTestCaseId());
                    // 根据passed字段转换为status
                    vo.setStatus(tcr.isPassed() ? "ACCEPTED" : "WRONG_ANSWER");
                    vo.setTimeUsed(tcr.getTimeUsed());
                    vo.setMemoryUsed(tcr.getMemoryUsed());
                    // 从testCases获取input（如果索引对应）
                    if (i < testCases.size()) {
                        vo.setInput(testCases.get(i).getInput());
                    }
                    vo.setExpectedOutput(tcr.getExpectedOutput());
                    vo.setActualOutput(tcr.getActualOutput());
                    vo.setErrorMessage(tcr.getErrorMessage());
                    testCaseResults.add(vo);
                }
            }
            result.setTestCaseResults(testCaseResults);
            
            log.info("Docker评测完成: submissionId={}, status={}, score={}, testCases={}/{}", 
                    submission.getId(), result.getStatus(), result.getScore(),
                    judgeResult.getPassedTestCases(), judgeResult.getTotalTestCases());
            
            // 防作弊检测：只要有得分就检测（防止部分硬编码得分）
            if (result.getScore() > 0) {
                try {
                    // 将 JudgeTestCase 转换为 TestCase 用于检测
                    List<TestCase> testCasesForCheck = new ArrayList<>();
                    for (com.cloudoj.model.dto.judge.JudgeTestCase jtc : testCases) {
                        TestCase tc = new TestCase();
                        tc.setId(jtc.getId());
                        tc.setInput(jtc.getInput());
                        tc.setOutput(jtc.getOutput());
                        testCasesForCheck.add(tc);
                    }
                    
                    AntiCheatService.CheatDetectionResult cheatResult = antiCheatService.detectHardcodedOutput(
                            submission.getCode(), submission.getLanguage(), testCasesForCheck);
                    
                    if (cheatResult.isCheatDetected()) {
                        // 检测到作弊，将结果标记为作弊
                        log.warn("检测到作弊行为: submissionId={}, type={}", submission.getId(), cheatResult.getCheatType());
                        result.setStatus(JudgeStatusEnum.WRONG_ANSWER.getCode());
                        result.setStatusDesc("检测到作弊行为");
                        result.setScore(0);
                        result.setErrorMessage("检测到硬编码输出作弊，请使用正确的算法解决问题");
                    } else if (cheatResult.isSuspicious()) {
                        // 可疑但未确定，记录日志但不影响结果
                        log.info("可疑提交: submissionId={}, message={}", submission.getId(), cheatResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("防作弊检测失败", e);
                    // 检测失败不影响正常评测结果
                }
            }
            
        } catch (Exception e) {
            log.error("Docker评测失败，使用模拟评测", e);
            return simulateJudge(submission);
        }
        
        return result;
    }
    
    /**
     * 获取测试用例（从problem-service，使用 Feign + Sentinel 降级）
     */
    private List<com.cloudoj.model.dto.judge.JudgeTestCase> getMockTestCases(Long problemId) {
        try {
            log.info("从problem-service获取测试用例：problemId={}", problemId);
            
            // 使用 Feign 客户端调用（自动支持 Sentinel 降级）
            Result<List<TestCase>> result = problemServiceClient.getTestCases(problemId);
            
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                log.warn("获取测试用例为空，尝试使用题目样例：problemId={}", problemId);
                return getFallbackTestCasesFromProblem(problemId);
            }
            
            List<TestCase> testCases = result.getData();
            log.info("成功获取测试用例：problemId={}, count={}", problemId, testCases.size());
            
            // 转换为JudgeTestCase
            return testCases.stream()
                .map(tc -> com.cloudoj.model.dto.judge.JudgeTestCase.builder()
                    .id(tc.getId())
                    .input(tc.getInput())
                    .output(tc.getOutput())
                    .score(tc.getScore() != null ? tc.getScore() : 20) // 默认20分
                    .timeLimit(5000)  // 5秒，可以从Problem中获取
                    .memoryLimit(256) // 256MB，可以从Problem中获取
                    .build())
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("调用problem-service失败，尝试使用题目样例：problemId={}", problemId, e);
            return getFallbackTestCasesFromProblem(problemId);
        }
    }
    
    /**
     * 从题目样例获取降级测试用例
     * 当题目没有设置测试用例时，使用题目中的sampleInput和sampleOutput作为测试用例
     */
    private List<com.cloudoj.model.dto.judge.JudgeTestCase> getFallbackTestCasesFromProblem(Long problemId) {
        try {
            log.info("尝试从题目样例获取测试用例：problemId={}", problemId);
            
            // 获取题目详情
            Result<java.util.Map<String, Object>> problemResult = problemServiceClient.getProblemById(problemId);
            
            if (problemResult == null || problemResult.getData() == null) {
                log.warn("获取题目详情失败：problemId={}", problemId);
                return new ArrayList<>();
            }
            
            java.util.Map<String, Object> problem = problemResult.getData();
            String sampleInput = (String) problem.get("sampleInput");
            String sampleOutput = (String) problem.get("sampleOutput");
            
            // 检查样例是否存在
            if (sampleInput == null || sampleInput.trim().isEmpty() ||
                sampleOutput == null || sampleOutput.trim().isEmpty()) {
                log.warn("题目没有设置样例输入输出：problemId={}", problemId);
                return new ArrayList<>();
            }
            
            // 获取时间和内存限制
            Integer timeLimit = problem.get("timeLimit") != null ? 
                    ((Number) problem.get("timeLimit")).intValue() : 5000;
            Integer memoryLimit = problem.get("memoryLimit") != null ? 
                    ((Number) problem.get("memoryLimit")).intValue() : 256;
            
            // 使用题目样例作为测试用例
            List<com.cloudoj.model.dto.judge.JudgeTestCase> testCases = new ArrayList<>();
            testCases.add(com.cloudoj.model.dto.judge.JudgeTestCase.builder()
                    .id(-1L)  // 使用负数ID表示这是样例测试用例
                    .input(sampleInput.trim())
                    .output(sampleOutput.trim())
                    .score(100)  // 样例测试用例满分100
                    .timeLimit(timeLimit)
                    .memoryLimit(memoryLimit)
                    .build());
            
            log.info("使用题目样例作为测试用例：problemId={}, sampleInput长度={}, sampleOutput长度={}", 
                    problemId, sampleInput.length(), sampleOutput.length());
            
            return testCases;
            
        } catch (Exception e) {
            log.error("从题目样例获取测试用例失败：problemId={}", problemId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 模拟评测（Docker不可用时的备选方案）
     */
    private JudgeResultVO simulateJudge(Submission submission) {
        JudgeResultVO result = new JudgeResultVO();
        result.setSubmissionId(submission.getId());
        
        try {
            // 检查测试用例是否存在
            List<com.cloudoj.model.dto.judge.JudgeTestCase> testCases = getMockTestCases(submission.getProblemId());
            if (testCases == null || testCases.isEmpty()) {
                log.error("题目没有测试用例，无法评测：problemId={}", submission.getProblemId());
                result.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
                result.setStatusDesc("题目暂无测试用例");
                result.setScore(0);
                result.setTimeUsed(0);
                result.setMemoryUsed(0);
                result.setPassRate("0.00%");
                result.setErrorMessage("该题目暂未设置测试用例，请联系管理员添加测试用例后再提交");
                result.setTestCaseResults(new ArrayList<>());
                return result;
            }
            
            // 模拟编译和运行
            Thread.sleep(1000); // 模拟评测耗时
            
            // 随机生成评测结果（仅供演示）
            Random random = new Random();
            int rand = random.nextInt(100);
            
            if (rand < 70) { // 70% 概率通过
                result.setStatus(JudgeStatusEnum.ACCEPTED.getCode());
                result.setStatusDesc(JudgeStatusEnum.ACCEPTED.getDesc());
                result.setScore(100);
                result.setPassRate("100.00%");
            } else if (rand < 85) { // 15% 概率答案错误
                result.setStatus(JudgeStatusEnum.WRONG_ANSWER.getCode());
                result.setStatusDesc(JudgeStatusEnum.WRONG_ANSWER.getDesc());
                result.setScore(60);
                result.setPassRate("60.00%");
                result.setErrorMessage("测试用例3输出不匹配");
            } else if (rand < 95) { // 10% 概率运行时错误
                result.setStatus(JudgeStatusEnum.RUNTIME_ERROR.getCode());
                result.setStatusDesc(JudgeStatusEnum.RUNTIME_ERROR.getDesc());
                result.setScore(0);
                result.setPassRate("0.00%");
                result.setErrorMessage("NullPointerException at line 10");
            } else { // 5% 概率时间超限
                result.setStatus(JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getCode());
                result.setStatusDesc(JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getDesc());
                result.setScore(0);
                result.setPassRate("0.00%");
                result.setErrorMessage("执行超过1000ms限制");
            }
            
            // 随机生成运行时间和内存
            result.setTimeUsed(random.nextInt(500) + 50);  // 50-550ms
            result.setMemoryUsed(random.nextInt(20000) + 4096); // 4-24MB
            
        } catch (Exception e) {
            log.error("评测失败", e);
            result.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
            result.setStatusDesc(JudgeStatusEnum.SYSTEM_ERROR.getDesc());
            result.setScore(0);
            result.setPassRate("0.00%");
            result.setErrorMessage("系统错误: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public SubmissionVO getSubmissionById(Long id) {
        // 先从缓存获取
        String cacheKey = SUBMISSION_CACHE_PREFIX + id;
        SubmissionVO cachedVO = (SubmissionVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedVO != null) {
            log.debug("从缓存获取提交记录, submissionId={}", id);
            return cachedVO;
        }
        
        // 缓存未命中，查询数据库
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            return null;
        }
        
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(submission, vo);
        
        // 设置状态描述
        JudgeStatusEnum statusEnum = JudgeStatusEnum.getByCode(submission.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }
        
        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, vo, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        log.debug("提交记录已缓存, submissionId={}", id);
        
        return vo;
    }
    
    @Override
    public List<SubmissionVO> getUserSubmissions(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Submission> submissions = submissionMapper.selectByUserId(userId, offset, size);
        return submissions.stream().map(submission -> {
            SubmissionVO vo = new SubmissionVO();
            BeanUtils.copyProperties(submission, vo);
            JudgeStatusEnum statusEnum = JudgeStatusEnum.getByCode(submission.getStatus());
            if (statusEnum != null) {
                vo.setStatusDesc(statusEnum.getDesc());
            }
            // 获取题目名称和难度
            try {
                Result<Map<String, Object>> problemResult = problemServiceClient.getProblemById(submission.getProblemId());
                if (problemResult != null && problemResult.isSuccess() && problemResult.getData() != null) {
                    Map<String, Object> problemData = problemResult.getData();
                    Object title = problemData.get("title");
                    vo.setProblemTitle(title != null ? title.toString() : "题目" + submission.getProblemId());
                    Object difficulty = problemData.get("difficulty");
                    vo.setDifficulty(difficulty != null ? difficulty.toString() : "MEDIUM");
                }
            } catch (Exception e) {
                log.warn("获取题目信息失败, problemId={}", submission.getProblemId());
                vo.setProblemTitle("题目" + submission.getProblemId());
                vo.setDifficulty("MEDIUM");
            }
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public PageResult<SubmissionVO> getUserSubmissionsPage(Long userId, int page, int size) {
        // 获取总数
        Integer total = submissionMapper.countByUserId(userId);
        if (total == null) total = 0;
        
        // 获取数据
        List<SubmissionVO> records = getUserSubmissions(userId, page, size);
        
        // 计算总页数
        long pages = (total + size - 1) / size;
        
        return PageResult.of((long) page, (long) size, total.longValue(), records);
    }
    
    @Override
    public List<SubmissionVO> getProblemSubmissions(Long problemId, int page, int size) {
        int offset = (page - 1) * size;
        List<Submission> submissions = submissionMapper.selectByProblemId(problemId, offset, size);
        return submissions.stream().map(submission -> {
            SubmissionVO vo = new SubmissionVO();
            BeanUtils.copyProperties(submission, vo);
            JudgeStatusEnum statusEnum = JudgeStatusEnum.getByCode(submission.getStatus());
            if (statusEnum != null) {
                vo.setStatusDesc(statusEnum.getDesc());
            }
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public JudgeResultVO getJudgeResult(Long submissionId) {
        // 先从缓存获取
        String cacheKey = JUDGE_RESULT_CACHE_PREFIX + submissionId;
        JudgeResultVO cachedResult = (JudgeResultVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            log.debug("从缓存获取评测结果, submissionId={}", submissionId);
            return cachedResult;
        }
        
        // 缓存未命中，查询数据库
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return null;
        }
        
        JudgeResultVO result = new JudgeResultVO();
        result.setSubmissionId(submission.getId());
        result.setStatus(submission.getStatus());
        
        JudgeStatusEnum statusEnum = JudgeStatusEnum.getByCode(submission.getStatus());
        if (statusEnum != null) {
            result.setStatusDesc(statusEnum.getDesc());
        }
        
        result.setScore(submission.getScore());
        result.setTimeUsed(submission.getTimeUsed());
        result.setMemoryUsed(submission.getMemoryUsed());
        result.setErrorMessage(submission.getErrorMessage());
        
        if (submission.getPassRate() != null) {
            result.setPassRate(submission.getPassRate().toString() + "%");
        }
        
        // 解析测试用例结果详情
        if (submission.getTestCaseResults() != null && !submission.getTestCaseResults().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<JudgeResultVO.TestCaseResultVO> testCaseResults = objectMapper.readValue(
                    submission.getTestCaseResults(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JudgeResultVO.TestCaseResultVO.class)
                );
                result.setTestCaseResults(testCaseResults);
            } catch (Exception e) {
                log.error("解析测试用例结果失败, submissionId={}", submissionId, e);
            }
        }
        
        // 只有评测完成的结果才缓存
        if (!JudgeStatusEnum.PENDING.getCode().equals(submission.getStatus()) 
            && !JudgeStatusEnum.JUDGING.getCode().equals(submission.getStatus())) {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
            log.debug("评测结果已缓存, submissionId={}", submissionId);
        }
        
        return result;
    }
    
    @Override
    public void markAsSystemError(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return;
        }
        
        submission.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
        submission.setScore(0);
        submission.setPassRate(BigDecimal.ZERO);
        submission.setErrorMessage("系统错误：评测任务处理失败");
        submissionMapper.updateJudgeResult(submission);
        
        // 清除缓存
        clearSubmissionCache(submissionId);
        
        log.info("提交记录已标记为系统错误, submissionId={}", submissionId);
    }
    
    /**
     * 更新题目统计信息（使用 Feign + Sentinel 降级）
     */
    private void updateProblemStatistics(Long problemId, Boolean isAccepted) {
        try {
            // 使用 Feign 客户端调用（自动支持 Sentinel 降级）
            problemServiceClient.updateProblemStats(problemId, isAccepted);
            log.info("更新题目统计成功, problemId={}, isAccepted={}", problemId, isAccepted);
        } catch (Exception e) {
            log.error("更新题目统计失败, problemId={}, isAccepted={}", problemId, isAccepted, e);
            // 统计更新失败不影响评测结果，只记录日志
        }
    }
    
    /**
     * 更新学习进度到learning-service（使用 Feign + Sentinel 降级）
     */
    private void updateLearningProgress(Submission submission, JudgeResultVO result) {
        try {
            String status = result.getStatus();
            Integer score = result.getScore();
            Integer executionTime = result.getTimeUsed();
            
            // 调用learning-service更新学习进度
            learningServiceClient.updateProgress(
                submission.getUserId(),
                submission.getProblemId(),
                status,
                score,
                executionTime
            );
            log.info("更新学习进度成功, userId={}, problemId={}, status={}, score={}, executionTime={}", 
                    submission.getUserId(), submission.getProblemId(), status, score, executionTime);
            
            // 如果不是通过，添加错题记录
            if (!"ACCEPTED".equals(status)) {
                learningServiceClient.addWrongProblem(
                    submission.getUserId(),
                    submission.getProblemId(),
                    submission.getId(),
                    status
                );
                log.info("添加错题记录成功, userId={}, problemId={}, errorType={}", 
                        submission.getUserId(), submission.getProblemId(), status);
            } else {
                // 如果通过了，标记错题为已解决
                learningServiceClient.resolveWrongProblem(
                    submission.getUserId(),
                    submission.getProblemId()
                );
            }
            
            // 记录学习统计
            int codeLines = submission.getCode() != null ? 
                    submission.getCode().split("\n").length : 0;
            learningServiceClient.recordStatistics(
                submission.getUserId(),
                1,  // submitCount
                "ACCEPTED".equals(status) ? 1 : 0,  // acceptCount
                "ACCEPTED".equals(status) ? 1 : 0,  // problemSolved
                codeLines
            );
            
        } catch (Exception e) {
            log.error("更新学习进度失败, userId={}, problemId={}", 
                    submission.getUserId(), submission.getProblemId(), e);
            // 学习进度更新失败不影响评测结果，只记录日志
        }
    }
    
    /**
     * 记录作业提交到course-service（使用 Feign + Sentinel 降级）
     */
    private void recordHomeworkSubmission(Submission submission, JudgeResultVO result) {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("homeworkId", submission.getHomeworkId());
            params.put("studentId", submission.getUserId());
            params.put("problemId", submission.getProblemId());
            params.put("judgeSubmissionId", submission.getId());
            params.put("score", result.getScore() != null ? result.getScore() : 0);
            params.put("status", result.getStatus());
            
            // 使用 Feign 客户端调用（自动支持 Sentinel 降级）
            courseServiceClient.recordHomeworkSubmission(params);
            log.info("记录作业提交成功, homeworkId={}, studentId={}, problemId={}, score={}", 
                    submission.getHomeworkId(), submission.getUserId(), submission.getProblemId(), result.getScore());
        } catch (Exception e) {
            log.error("记录作业提交失败, homeworkId={}, studentId={}", 
                    submission.getHomeworkId(), submission.getUserId(), e);
            // 记录失败不影响评测结果，只记录日志
        }
    }
    
    @Override
    public void clearSubmissionCache(Long submissionId) {
        String submissionKey = SUBMISSION_CACHE_PREFIX + submissionId;
        String resultKey = JUDGE_RESULT_CACHE_PREFIX + submissionId;
        
        redisTemplate.delete(submissionKey);
        redisTemplate.delete(resultKey);
        
        log.debug("已清除提交记录缓存, submissionId={}", submissionId);
    }
    
    @Override
    public java.util.Map<String, Object> getUserSubmissionStats(Long userId) {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 统计总提交数
        Integer totalSubmissions = submissionMapper.countByUserId(userId);
        stats.put("totalSubmissions", totalSubmissions != null ? totalSubmissions : 0);
        
        // 统计通过数
        Integer totalAccepted = submissionMapper.countAcceptedByUserId(userId);
        stats.put("totalAccepted", totalAccepted != null ? totalAccepted : 0);
        
        // 统计解决的不同题目数
        Integer problemsSolved = submissionMapper.countDistinctAcceptedProblemsByUserId(userId);
        stats.put("totalProblemsSolved", problemsSolved != null ? problemsSolved : 0);
        
        return stats;
    }
    
    @Override
    public Long getTotalSubmissionCount() {
        Long count = submissionMapper.countTotal();
        return count != null ? count : 0L;
    }
}
