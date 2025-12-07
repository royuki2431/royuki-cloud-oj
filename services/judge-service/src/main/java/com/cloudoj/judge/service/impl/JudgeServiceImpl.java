package com.cloudoj.judge.service.impl;

import com.cloudoj.judge.config.RabbitMQConfig;
import com.cloudoj.judge.mapper.SubmissionMapper;
import com.cloudoj.judge.service.JudgeService;
import com.cloudoj.model.dto.judge.JudgeMessage;
import com.cloudoj.model.dto.judge.SubmitCodeRequest;
import com.cloudoj.model.entity.judge.Submission;
import com.cloudoj.model.enums.JudgeStatusEnum;
import com.cloudoj.model.vo.judge.JudgeResultVO;
import com.cloudoj.model.vo.judge.SubmissionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
    
    // Redis缓存key前缀
    private static final String SUBMISSION_CACHE_PREFIX = "submission:";
    private static final String JUDGE_RESULT_CACHE_PREFIX = "judge:result:";
    // 缓存过期时间（秒）
    private static final long CACHE_EXPIRE_TIME = 300; // 5分钟
    
    @Override
    public Long submitCode(SubmitCodeRequest request, String ipAddress) {
        // 创建提交记录
        Submission submission = new Submission();
        submission.setProblemId(request.getProblemId());
        submission.setUserId(request.getUserId());
        submission.setLanguage(request.getLanguage());
        submission.setCode(request.getCode());
        submission.setStatus(JudgeStatusEnum.PENDING.getCode());
        submission.setIpAddress(ipAddress);
        
        // 保存到数据库
        submissionMapper.insert(submission);
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
        
        // 执行评测（这里是模拟评测，实际应该调用沙箱执行）
        JudgeResultVO result = simulateJudge(submission);
        
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
        
        submissionMapper.updateJudgeResult(submission);
        
        // 清除缓存，让下次查询能获取最新结果
        clearSubmissionCache(submissionId);
        
        log.info("评测完成, submissionId={}, status={}, score={}", 
                submissionId, result.getStatus(), result.getScore());
        
        return result;
    }
    
    /**
     * 模拟评测（TODO: 替换为真实的Docker沙箱评测）
     */
    private JudgeResultVO simulateJudge(Submission submission) {
        JudgeResultVO result = new JudgeResultVO();
        result.setSubmissionId(submission.getId());
        
        try {
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
            return vo;
        }).collect(Collectors.toList());
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
    
    @Override
    public void clearSubmissionCache(Long submissionId) {
        String submissionKey = SUBMISSION_CACHE_PREFIX + submissionId;
        String resultKey = JUDGE_RESULT_CACHE_PREFIX + submissionId;
        
        redisTemplate.delete(submissionKey);
        redisTemplate.delete(resultKey);
        
        log.debug("已清除提交记录缓存, submissionId={}", submissionId);
    }
}
