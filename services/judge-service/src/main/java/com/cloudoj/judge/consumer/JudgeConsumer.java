package com.cloudoj.judge.consumer;

import com.cloudoj.judge.config.RabbitMQConfig;
import com.cloudoj.judge.service.JudgeService;
import com.cloudoj.model.dto.judge.JudgeMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 评测消费者
 * 从RabbitMQ队列中消费评测任务并执行评测
 */
@Slf4j
@Component
public class JudgeConsumer {
    
    @Autowired
    private JudgeService judgeService;
    
    /**
     * 消费评测任务
     * 使用手动确认模式，确保消息处理的可靠性
     */
    @RabbitListener(queues = RabbitMQConfig.JUDGE_QUEUE)
    public void handleJudgeTask(JudgeMessage judgeMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        try {
            log.info("========================================");
            log.info("收到评测任务: submissionId={}, problemId={}, language={}", 
                    judgeMessage.getSubmissionId(), 
                    judgeMessage.getProblemId(), 
                    judgeMessage.getLanguage());
            
            // 执行评测
            judgeService.executeJudge(judgeMessage.getSubmissionId());
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("评测任务完成: submissionId={}", judgeMessage.getSubmissionId());
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("评测任务执行失败: submissionId={}, error={}", 
                    judgeMessage.getSubmissionId(), e.getMessage(), e);
            
            // 判断是否需要重试
            if (judgeMessage.getRetryCount() < 3) {
                // 拒绝消息并重新入队
                channel.basicNack(deliveryTag, false, true);
                log.warn("评测任务将重试: submissionId={}, retryCount={}", 
                        judgeMessage.getSubmissionId(), judgeMessage.getRetryCount());
            } else {
                // 超过重试次数，拒绝消息但不重新入队（进入死信队列）
                channel.basicNack(deliveryTag, false, false);
                log.error("评测任务失败，超过重试次数: submissionId={}", 
                        judgeMessage.getSubmissionId());
            }
        }
    }
    
    /**
     * 处理死信队列中的消息
     * 记录失败的评测任务
     */
    @RabbitListener(queues = RabbitMQConfig.JUDGE_DEAD_LETTER_QUEUE)
    public void handleDeadLetter(JudgeMessage judgeMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        log.error("========================================");
        log.error("收到死信消息: submissionId={}, problemId={}", 
                judgeMessage.getSubmissionId(), judgeMessage.getProblemId());
        log.error("该评测任务已失败，请人工处理");
        log.error("========================================");
        
        // 更新提交状态为系统错误
        try {
            judgeService.markAsSystemError(judgeMessage.getSubmissionId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("更新死信消息状态失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
