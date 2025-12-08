package com.cloudoj.judge.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 定义评测相关的队列、交换机和绑定关系
 */
@Configuration
public class RabbitMQConfig {
    
    // 队列名称常量
    public static final String JUDGE_QUEUE = "judge.queue";
    public static final String JUDGE_DEAD_LETTER_QUEUE = "judge.dead.letter.queue";
    
    // 交换机名称常量
    public static final String JUDGE_EXCHANGE = "judge.exchange";
    public static final String JUDGE_DEAD_LETTER_EXCHANGE = "judge.dead.letter.exchange";
    
    // 路由键常量
    public static final String JUDGE_ROUTING_KEY = "judge.routing.key";
    public static final String JUDGE_DEAD_LETTER_ROUTING_KEY = "judge.dead.letter.routing.key";
    
    /**
     * 评测队列
     * 持久化队列，并配置死信交换机
     */
    @Bean
    public Queue judgeQueue() {
        return new Queue(JUDGE_QUEUE, true);
    }
    
    /**
     * 死信队列
     * 用于处理评测失败的消息
     */
    @Bean
    public Queue judgeDeadLetterQueue() {
        return new Queue(JUDGE_DEAD_LETTER_QUEUE, true);
    }
    
    /**
     * 评测交换机
     */
    @Bean
    public TopicExchange judgeExchange() {
        return new TopicExchange(JUDGE_EXCHANGE, true, false);
    }
    
    /**
     * 死信交换机
     */
    @Bean
    public TopicExchange judgeDeadLetterExchange() {
        return new TopicExchange(JUDGE_DEAD_LETTER_EXCHANGE, true, false);
    }
    
    /**
     * 绑定评测队列到交换机
     */
    @Bean
    public Binding judgeBinding() {
        return BindingBuilder
                .bind(judgeQueue())
                .to(judgeExchange())
                .with(JUDGE_ROUTING_KEY);
    }
    
    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding judgeDeadLetterBinding() {
        return BindingBuilder
                .bind(judgeDeadLetterQueue())
                .to(judgeDeadLetterExchange())
                .with(JUDGE_DEAD_LETTER_ROUTING_KEY);
    }
    
    /**
     * 消息转换器
     * 使用Jackson将消息转换为JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
