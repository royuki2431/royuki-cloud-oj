package com.cloudoj.learning.service;

import com.cloudoj.learning.entity.WrongProblem;

import java.util.List;
import java.util.Map;

/**
 * 错题本服务接口
 */
public interface WrongProblemService {
    
    /**
     * 添加错题记录
     */
    void addWrongProblem(Long userId, Long problemId, Long submissionId, String errorType);
    
    /**
     * 获取用户错题列表
     */
    List<WrongProblem> getUserWrongProblems(Long userId);
    
    /**
     * 获取用户未解决的错题列表
     */
    List<WrongProblem> getUnresolvedWrongProblems(Long userId);
    
    /**
     * 标记错题为已解决
     */
    void resolveWrongProblem(Long userId, Long problemId);
    
    /**
     * 删除错题记录
     */
    void deleteWrongProblem(Long id, Long userId);
    
    /**
     * 获取错题统计
     */
    Map<String, Object> getWrongProblemStatistics(Long userId);
    
    /**
     * 根据错误类型获取错题
     */
    List<WrongProblem> getWrongProblemsByErrorType(Long userId, String errorType);
}
