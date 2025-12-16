package com.cloudoj.judge.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.cloudoj.common.annotation.RequireLogin;
import com.cloudoj.common.annotation.RequireRole;
import com.cloudoj.common.context.UserContext;
import com.cloudoj.judge.service.JudgeService;
import com.cloudoj.model.common.PageResult;
import com.cloudoj.model.common.Result;
import com.cloudoj.model.dto.judge.SubmitCodeRequest;
import com.cloudoj.model.vo.judge.JudgeResultVO;
import com.cloudoj.model.vo.judge.SubmissionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评测服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/judge")
public class JudgeController {
    
    @Autowired
    private JudgeService judgeService;
    
    @Autowired
    private com.cloudoj.judge.service.SubmitRateLimiter submitRateLimiter;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Judge Service is running!";
    }
    
    /**
     * 提交代码（需要登录）
     * 使用 Sentinel 限流保护
     */
    @RequireLogin
    @PostMapping("/submit")
    @SentinelResource(value = "submitCode", blockHandler = "submitCodeBlockHandler", fallback = "submitCodeFallback")
    public Result<Long> submitCode(@Valid @RequestBody SubmitCodeRequest request, 
                                     HttpServletRequest httpRequest) {
        // 从用户上下文获取用户ID
        Long userId = UserContext.getUserId();
        if (userId != null) {
            request.setUserId(userId);
        } else if (request.getUserId() == null) {
            return Result.error("请先登录");
        }
        
        // 获取IP地址
        String ipAddress = getClientIp(httpRequest);
        
        // 提交代码
        Long submissionId = judgeService.submitCode(request, ipAddress);
        
        return Result.success(submissionId);
    }
    
    /**
     * 查询提交记录详情
     */
    @GetMapping("/submission/{id}")
    public Result<SubmissionVO> getSubmission(@PathVariable Long id) {
        SubmissionVO submission = judgeService.getSubmissionById(id);
        if (submission == null) {
            return Result.error("提交记录不存在");
        }
        return Result.success(submission);
    }
    
    /**
     * 查询评测结果
     */
    @GetMapping("/result/{submissionId}")
    public Result<JudgeResultVO> getJudgeResult(@PathVariable Long submissionId) {
        JudgeResultVO result = judgeService.getJudgeResult(submissionId);
        if (result == null) {
            return Result.error("评测结果不存在");
        }
        return Result.success(result);
    }
    
    /**
     * 查询用户的提交记录列表
     */
    @GetMapping("/submissions/user/{userId}")
    public PageResult<SubmissionVO> getUserSubmissions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return judgeService.getUserSubmissionsPage(userId, page, size);
    }
    
    /**
     * 查询题目的提交记录列表
     */
    @GetMapping("/submissions/problem/{problemId}")
    public Result<List<SubmissionVO>> getProblemSubmissions(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<SubmissionVO> submissions = judgeService.getProblemSubmissions(problemId, page, size);
        return Result.success(submissions);
    }
    
    /**
     * 重新评测（需要管理员权限）
     */
    @RequireRole({"ADMIN", "SUPER_ADMIN", "TEACHER"})
    @PostMapping("/rejudge/{submissionId}")
    public Result<JudgeResultVO> rejudge(@PathVariable Long submissionId) {
        JudgeResultVO result = judgeService.executeJudge(submissionId);
        return Result.success(result);
    }
    
    /**
     * 获取剩余提交次数
     */
    @GetMapping("/rate-limit/{userId}")
    public Result<Integer> getRemainingSubmits(@PathVariable Long userId) {
        int remaining = submitRateLimiter.getRemainingSubmits(userId);
        return Result.success(remaining);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    // ==================== 统计接口 ====================
    
    /**
     * 获取用户提交统计
     */
    @GetMapping("/statistics/user/{userId}")
    public Result<java.util.Map<String, Object>> getUserSubmissionStats(@PathVariable Long userId) {
        log.info("查询用户提交统计：userId={}", userId);
        java.util.Map<String, Object> stats = judgeService.getUserSubmissionStats(userId);
        return Result.success(stats);
    }
    
    // ==================== Sentinel 限流降级处理 ====================
    
    /**
     * 提交代码限流处理
     */
    public Result<Long> submitCodeBlockHandler(SubmitCodeRequest request, HttpServletRequest httpRequest, BlockException ex) {
        log.warn("提交代码触发限流: userId={}, rule={}", request.getUserId(), ex.getRule());
        
        java.util.Map<String, Object> errorInfo = new java.util.HashMap<>();
        errorInfo.put("type", "FLOW_LIMIT");
        errorInfo.put("resource", "submitCode");
        errorInfo.put("retryAfter", 3);
        errorInfo.put("hint", "提交过于频繁，请等待几秒后重试");
        
        return Result.error(429, "提交过于频繁，请稍后重试");
    }
    
    /**
     * 提交代码降级处理
     */
    public Result<Long> submitCodeFallback(SubmitCodeRequest request, HttpServletRequest httpRequest, Throwable ex) {
        log.error("提交代码服务降级: userId={}, error={}", request.getUserId(), ex.getMessage());
        
        java.util.Map<String, Object> errorInfo = new java.util.HashMap<>();
        errorInfo.put("type", "SERVICE_DEGRADE");
        errorInfo.put("resource", "submitCode");
        errorInfo.put("retryAfter", 10);
        errorInfo.put("hint", "评测服务正在恢复中，请稍后重试");
        
        return Result.error(503, "评测服务暂时不可用，请稍后重试");
    }
    
    /**
     * 查询结果限流处理
     */
    public Result<JudgeResultVO> getResultBlockHandler(Long submissionId, BlockException ex) {
        log.warn("查询评测结果触发限流: submissionId={}", submissionId);
        
        java.util.Map<String, Object> errorInfo = new java.util.HashMap<>();
        errorInfo.put("type", "FLOW_LIMIT");
        errorInfo.put("resource", "getResult");
        errorInfo.put("retryAfter", 2);
        errorInfo.put("hint", "查询过于频繁，请稍等片刻");
        
        return Result.error(429, "查询过于频繁，请稍后重试");
    }
}
