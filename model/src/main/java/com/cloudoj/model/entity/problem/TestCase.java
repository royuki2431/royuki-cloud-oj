package com.cloudoj.model.entity.problem;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试用例实体类
 */
@Data
public class TestCase implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 测试用例ID
     */
    private Long id;
    
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 输入数据
     */
    private String input;
    
    /**
     * 预期输出
     */
    private String output;
    
    /**
     * 是否为样例：1-是 0-否
     */
    private Integer isSample;
    
    /**
     * 分值
     */
    private Integer score;
    
    /**
     * 排序序号
     */
    private Integer orderNum;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
