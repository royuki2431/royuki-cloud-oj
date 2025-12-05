package com.cloudoj.model.entity.judge;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交记录实体类
 */
@Data
public class Submission implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 提交ID
     */
    private Long id;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 提交代码
     */
    private String code;
    
    /**
     * 评测状态
     */
    private String status;
    
    /**
     * 得分
     */
    private Integer score;
    
    /**
     * 运行时间（毫秒）
     */
    private Integer timeUsed;
    
    /**
     * 内存使用（KB）
     */
    private Integer memoryUsed;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 通过率
     */
    private BigDecimal passRate;
    
    /**
     * 测试用例结果详情（JSON）
     */
    private String testCaseResults;
    
    /**
     * 提交IP
     */
    private String ipAddress;
    
    /**
     * 提交时间
     */
    private LocalDateTime createTime;
    
    /**
     * 判题完成时间
     */
    private LocalDateTime judgedTime;
    
    /**
     * 逻辑删除标志：0-未删除 1-已删除
     */
    private Integer deleted;
}
