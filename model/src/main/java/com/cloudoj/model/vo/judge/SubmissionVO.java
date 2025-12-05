package com.cloudoj.model.vo.judge;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交记录视图对象
 */
@Data
public class SubmissionVO implements Serializable {
    
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
     * 题目标题（关联查询）
     */
    private String problemTitle;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名（关联查询）
     */
    private String username;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 提交代码（查询详情时返回）
     */
    private String code;
    
    /**
     * 评测状态
     */
    private String status;
    
    /**
     * 评测状态描述
     */
    private String statusDesc;
    
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
     * 提交时间
     */
    private LocalDateTime createTime;
    
    /**
     * 判题完成时间
     */
    private LocalDateTime judgedTime;
}
