package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Learning 服务降级工厂
 * 当 learning-service 不可用时，返回降级数据
 */
@Slf4j
@Component
public class LearningServiceFallbackFactory implements FallbackFactory<LearningServiceClient> {
    
    @Override
    public LearningServiceClient create(Throwable cause) {
        log.error("learning-service 调用失败，触发降级: {}", cause.getMessage());
        
        return new LearningServiceClient() {
            @Override
            public Result<Void> updateProgress(Long userId, Long problemId, String status, Integer score, Integer executionTime) {
                log.warn("更新学习进度降级处理: userId={}, problemId={}, status={}", userId, problemId, status);
                // 学习进度更新失败不影响主流程，返回成功
                return Result.success(null);
            }
            
            @Override
            public Result<Void> addWrongProblem(Long userId, Long problemId, Long submissionId, String errorType) {
                log.warn("添加错题记录降级处理: userId={}, problemId={}, errorType={}", userId, problemId, errorType);
                return Result.success(null);
            }
            
            @Override
            public Result<Void> resolveWrongProblem(Long userId, Long problemId) {
                log.warn("标记错题已解决降级处理: userId={}, problemId={}", userId, problemId);
                return Result.success(null);
            }
            
            @Override
            public Result<Void> recordStatistics(Long userId, int submitCount, int acceptCount, int problemSolved, int codeLines) {
                log.warn("记录学习统计降级处理: userId={}", userId);
                return Result.success(null);
            }
        };
    }
}
