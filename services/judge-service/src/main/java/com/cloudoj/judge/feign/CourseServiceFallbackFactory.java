package com.cloudoj.judge.feign;

import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Course 服务降级工厂
 * 当 course-service 不可用时，返回降级数据
 */
@Slf4j
@Component
public class CourseServiceFallbackFactory implements FallbackFactory<CourseServiceClient> {
    
    @Override
    public CourseServiceClient create(Throwable cause) {
        log.error("course-service 调用失败，触发降级: {}", cause.getMessage());
        
        return new CourseServiceClient() {
            @Override
            public Result<Void> recordHomeworkSubmission(Map<String, Object> params) {
                log.warn("记录作业提交降级处理: params={}", params);
                // 作业记录失败不影响主流程，返回成功
                return Result.success(null);
            }
        };
    }
}
