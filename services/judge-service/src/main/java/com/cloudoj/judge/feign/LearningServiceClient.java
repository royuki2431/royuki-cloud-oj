package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Learning 服务 Feign 客户端
 * 用于在评测完成后更新学习进度
 */
@FeignClient(
    name = "learning-service",
    fallbackFactory = LearningServiceFallbackFactory.class
)
public interface LearningServiceClient {
    
    /**
     * 更新学习进度
     */
    @PostMapping("/learning/progress/update")
    Result<Void> updateProgress(
        @RequestParam("userId") Long userId,
        @RequestParam("problemId") Long problemId,
        @RequestParam("status") String status,
        @RequestParam(value = "score", required = false) Integer score,
        @RequestParam(value = "executionTime", required = false) Integer executionTime
    );
    
    /**
     * 添加错题记录
     */
    @PostMapping("/learning/wrong/add")
    Result<Void> addWrongProblem(
        @RequestParam("userId") Long userId,
        @RequestParam("problemId") Long problemId,
        @RequestParam("submissionId") Long submissionId,
        @RequestParam("errorType") String errorType
    );
    
    /**
     * 标记错题已解决
     */
    @PostMapping("/learning/wrong/resolve")
    Result<Void> resolveWrongProblem(
        @RequestParam("userId") Long userId,
        @RequestParam("problemId") Long problemId
    );
    
    /**
     * 记录学习统计
     */
    @PostMapping("/learning/statistics/record")
    Result<Void> recordStatistics(
        @RequestParam("userId") Long userId,
        @RequestParam(value = "submitCount", defaultValue = "0") int submitCount,
        @RequestParam(value = "acceptCount", defaultValue = "0") int acceptCount,
        @RequestParam(value = "problemSolved", defaultValue = "0") int problemSolved,
        @RequestParam(value = "codeLines", defaultValue = "0") int codeLines
    );
}
