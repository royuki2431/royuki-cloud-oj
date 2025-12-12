package com.cloudoj.common.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Sentinel 限流降级统一处理类
 */
@Slf4j
public class SentinelBlockHandler {
    
    // 错误码定义
    public static final int CODE_FLOW_LIMIT = 429;
    public static final int CODE_DEGRADE = 503;
    public static final int CODE_PARAM_FLOW = 429;
    public static final int CODE_AUTHORITY = 403;
    public static final int CODE_SYSTEM_BUSY = 429;
    
    /**
     * 通用限流处理（返回详细信息）
     */
    public static Result<?> handleFlowException(BlockException ex) {
        String ruleName = ex.getRule() != null ? ex.getRule().getResource() : "unknown";
        log.warn("触发限流规则: resource={}, rule={}", ruleName, ex.getRule());
        
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("type", getBlockType(ex));
        errorInfo.put("resource", ruleName);
        errorInfo.put("retryAfter", 3); // 建议重试时间（秒）
        
        if (ex instanceof FlowException) {
            return Result.error(CODE_FLOW_LIMIT, "请求过于频繁，请稍后重试", errorInfo);
        }
        if (ex instanceof DegradeException) {
            errorInfo.put("retryAfter", 10);
            return Result.error(CODE_DEGRADE, "服务暂时不可用，系统正在恢复中", errorInfo);
        }
        if (ex instanceof ParamFlowException) {
            return Result.error(CODE_PARAM_FLOW, "操作过于频繁，请稍后重试", errorInfo);
        }
        if (ex instanceof AuthorityException) {
            errorInfo.put("retryAfter", 0);
            return Result.error(CODE_AUTHORITY, "无权访问该资源", errorInfo);
        }
        
        return Result.error(CODE_SYSTEM_BUSY, "系统繁忙，请稍后重试", errorInfo);
    }
    
    /**
     * 通用降级处理（返回详细信息）
     */
    public static Result<?> handleFallback(Throwable ex) {
        log.error("服务降级: {}", ex.getMessage());
        
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("type", "FALLBACK");
        errorInfo.put("reason", getSimpleErrorMessage(ex));
        errorInfo.put("retryAfter", 5);
        
        return Result.error(CODE_DEGRADE, "服务暂时不可用，请稍后重试", errorInfo);
    }
    
    /**
     * 获取限流类型
     */
    private static String getBlockType(BlockException ex) {
        if (ex instanceof FlowException) return "FLOW_LIMIT";
        if (ex instanceof DegradeException) return "DEGRADE";
        if (ex instanceof ParamFlowException) return "PARAM_FLOW";
        if (ex instanceof AuthorityException) return "AUTHORITY";
        return "BLOCK";
    }
    
    /**
     * 获取简化的错误信息
     */
    private static String getSimpleErrorMessage(Throwable ex) {
        if (ex == null) return "unknown";
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) return ex.getClass().getSimpleName();
        // 截取前100个字符
        return msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
    }
}
