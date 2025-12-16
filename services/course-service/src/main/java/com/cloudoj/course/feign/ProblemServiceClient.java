package com.cloudoj.course.feign;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 题目服务Feign客户端
 */
@FeignClient(name = "problem-service", fallbackFactory = ProblemServiceFallbackFactory.class)
public interface ProblemServiceClient {
    
    /**
     * 根据ID获取题目详情
     */
    @GetMapping("/problem/{id}")
    Result<Problem> getProblemById(@PathVariable("id") Long id);
}
