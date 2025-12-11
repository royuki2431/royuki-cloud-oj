package com.cloudoj.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sentinel 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class SentinelExceptionHandler {
    
    /**
     * 处理 Sentinel 限流异常
     */
    @ExceptionHandler(FlowException.class)
    public Result<?> handleFlowException(FlowException e) {
        log.warn("触发流量控制: rule={}", e.getRule());
        return Result.error(429, "请求过于频繁，请稍后重试");
    }
    
    /**
     * 处理 Sentinel 熔断降级异常
     */
    @ExceptionHandler(DegradeException.class)
    public Result<?> handleDegradeException(DegradeException e) {
        log.warn("触发熔断降级: rule={}", e.getRule());
        return Result.error(503, "服务暂时不可用，请稍后重试");
    }
    
    /**
     * 处理 Sentinel 热点参数限流异常
     */
    @ExceptionHandler(ParamFlowException.class)
    public Result<?> handleParamFlowException(ParamFlowException e) {
        log.warn("触发热点参数限流: rule={}", e.getRule());
        return Result.error(429, "请求参数限流，请稍后重试");
    }
    
    /**
     * 处理 Sentinel 授权异常
     */
    @ExceptionHandler(AuthorityException.class)
    public Result<?> handleAuthorityException(AuthorityException e) {
        log.warn("触发授权规则: rule={}", e.getRule());
        return Result.error(403, "无权访问该资源");
    }
    
    /**
     * 处理所有 Sentinel Block 异常
     */
    @ExceptionHandler(BlockException.class)
    public Result<?> handleBlockException(BlockException e) {
        log.warn("触发 Sentinel 规则: rule={}", e.getRule());
        return Result.error(429, "系统繁忙，请稍后重试");
    }
}
