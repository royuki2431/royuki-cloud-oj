package com.cloudoj.judge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 使用 STOMP 协议进行消息传递
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理
        // /topic 用于广播消息（如公告）
        // /queue 用于点对点消息（如评测结果推送给特定用户）
        config.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 用户目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点，客户端通过此端点连接 WebSocket
        registry.addEndpoint("/ws/judge")
                .setAllowedOriginPatterns("*")  // 允许跨域
                .withSockJS();  // 支持 SockJS 降级
        
        // 也提供原生 WebSocket 端点（不使用 SockJS）
        registry.addEndpoint("/ws/judge")
                .setAllowedOriginPatterns("*");
    }
}
