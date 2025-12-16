package com.cloudoj.course.feign;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 题目服务降级工厂
 */
@Slf4j
@Component
public class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {
    
    @Override
    public ProblemServiceClient create(Throwable cause) {
        log.error("调用题目服务失败: {}", cause.getMessage());
        return new ProblemServiceClient() {
            @Override
            public Result<Problem> getProblemById(Long id) {
                log.warn("获取题目详情降级处理: problemId={}", id);
                return Result.error("题目服务暂不可用");
            }
        };
    }
}
