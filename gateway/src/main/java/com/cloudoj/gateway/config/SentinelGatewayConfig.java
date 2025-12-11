package com.cloudoj.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流规则配置
 * 注意：SentinelGatewayFilter 和 SentinelGatewayBlockExceptionHandler 
 * 由 spring-cloud-alibaba-sentinel-gateway 自动配置，无需手动定义
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {
    
    /**
     * 初始化限流规则
     */
    @PostConstruct
    public void initGatewayRules() {
        log.info("初始化 Sentinel 网关限流规则...");
        
        // 定义 API 分组
        initCustomizedApis();
        
        // 定义限流规则
        initFlowRules();
        
        log.info("Sentinel 网关限流规则初始化完成");
    }
    
    /**
     * 自定义 API 分组
     */
    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();
        
        // 评测接口分组
        ApiDefinition judgeApi = new ApiDefinition("judge-api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/judge/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(judgeApi);
        
        // 用户接口分组
        ApiDefinition userApi = new ApiDefinition("user-api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/user/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(userApi);
        
        // 题目接口分组
        ApiDefinition problemApi = new ApiDefinition("problem-api")
                .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                    add(new ApiPathPredicateItem()
                            .setPattern("/api/problem/**")
                            .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
                }});
        definitions.add(problemApi);
        
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }
    
    /**
     * 初始化限流规则
     */
    private void initFlowRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        
        // 评测接口限流：每秒最多 50 个请求
        rules.add(new GatewayFlowRule("judge-api")
                .setCount(50)
                .setIntervalSec(1));
        
        // 用户接口限流：每秒最多 100 个请求
        rules.add(new GatewayFlowRule("user-api")
                .setCount(100)
                .setIntervalSec(1));
        
        // 题目接口限流：每秒最多 200 个请求
        rules.add(new GatewayFlowRule("problem-api")
                .setCount(200)
                .setIntervalSec(1));
        
        // 按路由限流：judge-service 路由每秒最多 100 个请求
        rules.add(new GatewayFlowRule("judge-service")
                .setCount(100)
                .setIntervalSec(1)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID));
        
        // 按路由限流：user-service 路由每秒最多 200 个请求
        rules.add(new GatewayFlowRule("user-service")
                .setCount(200)
                .setIntervalSec(1)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID));
        
        GatewayRuleManager.loadRules(rules);
        
        log.info("已加载 {} 条网关限流规则", rules.size());
    }
}
