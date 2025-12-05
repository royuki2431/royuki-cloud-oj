package com.cloudoj.model.vo.judge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 评测结果视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResultVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 提交ID
     */
    private Long submissionId;
    
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
     * 通过率
     */
    private String passRate;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
