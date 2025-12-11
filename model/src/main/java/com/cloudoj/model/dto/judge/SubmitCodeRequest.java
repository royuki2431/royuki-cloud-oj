package com.cloudoj.model.dto.judge;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 提交代码请求DTO
 */
@Data
public class SubmitCodeRequest {
    
    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Long problemId;
    
    /**
     * 用户ID（从JWT中获取）
     */
    private Long userId;
    
    /**
     * 编程语言：JAVA/C/CPP/PYTHON
     */
    @NotBlank(message = "编程语言不能为空")
    private String language;
    
    /**
     * 提交代码
     */
    @NotBlank(message = "代码不能为空")
    private String code;
    
    /**
     * 作业ID（可选，如果是作业提交则需要填写）
     */
    private Long homeworkId;
}
