package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Course 服务 Feign 客户端
 * 使用 Sentinel 降级
 */
@FeignClient(
    name = "course-service",
    fallbackFactory = CourseServiceFallbackFactory.class
)
public interface CourseServiceClient {
    
    /**
     * 记录作业提交
     */
    @PostMapping("/course/homework/recordSubmission")
    Result<Void> recordHomeworkSubmission(@RequestBody Map<String, Object> params);
}
