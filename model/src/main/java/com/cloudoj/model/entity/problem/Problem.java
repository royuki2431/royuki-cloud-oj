package com.cloudoj.model.entity.problem;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目实体类
 */
@Data
public class Problem implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 题目ID
     */
    private Long id;
    
    /**
     * 题目标题
     */
    private String title;
    
    /**
     * 题目描述
     */
    private String description;
    
    /**
     * 输入格式说明
     */
    private String inputFormat;
    
    /**
     * 输出格式说明
     */
    private String outputFormat;
    
    /**
     * 样例输入
     */
    private String sampleInput;
    
    /**
     * 样例输出
     */
    private String sampleOutput;
    
    /**
     * 提示信息
     */
    private String hint;
    
    /**
     * 难度：EASY/MEDIUM/HARD
     */
    private String difficulty;
    
    /**
     * 题目分类
     */
    private String category;
    
    /**
     * 题目标签（JSON数组）
     */
    private String tags;
    
    /**
     * 时间限制（毫秒）
     */
    private Integer timeLimit;
    
    /**
     * 内存限制（MB）
     */
    private Integer memoryLimit;
    
    /**
     * 支持的语言
     */
    private String languages;
    
    /**
     * 题目来源
     */
    private String source;
    
    /**
     * 创建者ID
     */
    private Long authorId;
    
    /**
     * 通过次数
     */
    private Integer acceptCount;
    
    /**
     * 提交次数
     */
    private Integer submitCount;
    
    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;
    
    /**
     * 是否公开：1-是 0-否
     */
    private Integer isPublic;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标志：0-未删除 1-已删除
     */
    private Integer deleted;
}
