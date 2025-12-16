package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.TestCase;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Problem 服务 Feign 客户端
 * 使用 Sentinel 降级
 */
@FeignClient(
    name = "problem-service",
    fallbackFactory = ProblemServiceFallbackFactory.class
)
public interface ProblemServiceClient {
    
    /**
     * 获取题目详情
     */
    @GetMapping("/problem/{problemId}")
    Result<Map<String, Object>> getProblemById(@PathVariable("problemId") Long problemId);
    
    /**
     * 获取题目的测试用例
     */
    @GetMapping("/problem/{problemId}/testcases")
    Result<List<TestCase>> getTestCases(@PathVariable("problemId") Long problemId);
    
    /**
     * 更新题目统计信息
     */
    @PostMapping("/problem/updateStats/{problemId}")
    Result<Void> updateProblemStats(
        @PathVariable("problemId") Long problemId,
        @RequestParam("isAccepted") Boolean isAccepted
    );
}
