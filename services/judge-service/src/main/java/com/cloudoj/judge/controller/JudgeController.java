package com.cloudoj.judge.controller;

import com.cloudoj.judge.service.JudgeService;
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
     * 提交代码
     */
    @PostMapping("/submit")
    public Result<Long> submitCode(@Valid @RequestBody SubmitCodeRequest request, 
                                     HttpServletRequest httpRequest) {
        // 模拟从JWT中获取用户ID（实际应该从Token解析）
        if (request.getUserId() == null) {
            request.setUserId(1L); // 默认用户ID
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
    public Result<List<SubmissionVO>> getUserSubmissions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<SubmissionVO> submissions = judgeService.getUserSubmissions(userId, page, size);
        return Result.success(submissions);
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
     * 重新评测
     */
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
}
