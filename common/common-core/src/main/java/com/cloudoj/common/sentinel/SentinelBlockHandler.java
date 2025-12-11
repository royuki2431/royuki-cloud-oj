package com.cloudoj.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;

/**
 * Sentinel 限流降级统一处理类
 */
@Slf4j
public class SentinelBlockHandler {
    
    /**
     * 通用限流处理
     */
    public static Result<?> handleFlowException(BlockException ex) {
        log.warn("触发限流: {}", ex.getRule());
        
        if (ex instanceof FlowException) {
            return Result.error(429, "请求过于频繁，请稍后重试");
        }
        if (ex instanceof DegradeException) {
            return Result.error(503, "服务暂时不可用，请稍后重试");
        }
        if (ex instanceof ParamFlowException) {
            return Result.error(429, "热点参数限流，请稍后重试");
        }
        if (ex instanceof AuthorityException) {
            return Result.error(403, "无权访问该资源");
        }
        
        return Result.error(429, "系统繁忙，请稍后重试");
    }
    
    /**
     * 通用降级处理
     */
    public static Result<?> handleFallback(Throwable ex) {
        log.error("服务降级: {}", ex.getMessage());
        return Result.error(503, "服务暂时不可用，请稍后重试");
    }
}
