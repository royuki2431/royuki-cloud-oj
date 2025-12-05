package com.cloudoj.model.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应结果类
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> {
    
    /**
     * 当前页码
     */
    private Long current;
    
    /**
     * 每页大小
     */
    private Long size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Long pages;
    
    public PageResult() {
        super();
    }
    
    public PageResult(Long current, Long size, Long total, List<T> records) {
        super(200, "查询成功", records);
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = (total + size - 1) / size;
    }
    
    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }
}
