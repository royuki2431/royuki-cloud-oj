package com.cloudoj.judge.service;

import com.cloudoj.model.vo.judge.JudgeResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 评测结果通知服务
 * 通过 WebSocket 推送评测结果给前端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 推送评测结果给指定用户
     * 
     * @param userId 用户ID
     * @param result 评测结果
     */
    public void notifyUser(Long userId, JudgeResultVO result) {
        try {
            // 推送到用户专属 topic: /topic/judge-result/{userId}
            String destination = "/topic/judge-result/" + userId;
            messagingTemplate.convertAndSend(destination, result);
            log.info("评测结果已推送: userId={}, submissionId={}, status={}, destination={}", 
                    userId, result.getSubmissionId(), result.getStatus(), destination);
        } catch (Exception e) {
            log.error("推送评测结果失败: userId={}, submissionId={}", 
                    userId, result.getSubmissionId(), e);
        }
    }
    
    /**
     * 推送评测状态更新（如：开始评测、评测中等）
     * 
     * @param userId 用户ID
     * @param submissionId 提交ID
     * @param status 状态
     * @param message 消息
     */
    public void notifyStatus(Long userId, Long submissionId, String status, String message) {
        try {
            JudgeStatusMessage statusMessage = new JudgeStatusMessage();
            statusMessage.setSubmissionId(submissionId);
            statusMessage.setStatus(status);
            statusMessage.setMessage(message);
            statusMessage.setTimestamp(System.currentTimeMillis());
            
            // 推送到用户专属 topic: /topic/judge-status/{userId}
            String destination = "/topic/judge-status/" + userId;
            messagingTemplate.convertAndSend(destination, statusMessage);
            log.debug("评测状态已推送: userId={}, submissionId={}, status={}, destination={}", 
                    userId, submissionId, status, destination);
        } catch (Exception e) {
            log.error("推送评测状态失败: userId={}, submissionId={}", 
                    userId, submissionId, e);
        }
    }
    
    /**
     * 广播消息（如系统公告）
     * 
     * @param message 消息内容
     */
    public void broadcast(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/announcements", message);
            log.info("广播消息已发送: {}", message);
        } catch (Exception e) {
            log.error("广播消息失败: {}", message, e);
        }
    }
    
    /**
     * 评测状态消息
     */
    @lombok.Data
    public static class JudgeStatusMessage {
        private Long submissionId;
        private String status;
        private String message;
        private Long timestamp;
    }
}
