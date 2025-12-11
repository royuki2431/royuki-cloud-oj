package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Problem 服务降级工厂
 * 当 problem-service 不可用时，返回降级数据
 */
@Slf4j
@Component
public class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {
    
    @Override
    public ProblemServiceClient create(Throwable cause) {
        log.error("problem-service 调用失败，触发降级: {}", cause.getMessage());
        
        return new ProblemServiceClient() {
            @Override
            public Result<List<TestCase>> getTestCases(Long problemId) {
                log.warn("获取测试用例降级处理: problemId={}", problemId);
                // 返回空列表，让评测服务使用默认测试用例
                return Result.success(new ArrayList<>());
            }
            
            @Override
            public Result<Void> updateProblemStats(Long problemId, Boolean isAccepted) {
                log.warn("更新题目统计降级处理: problemId={}, isAccepted={}", problemId, isAccepted);
                // 统计更新失败不影响主流程，返回成功
                return Result.success(null);
            }
        };
    }
}
