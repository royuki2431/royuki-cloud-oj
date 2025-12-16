package com.cloudoj.judge.mapper;

import com.cloudoj.model.entity.judge.Submission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 提交记录Mapper接口
 */
public interface SubmissionMapper {
    
    /**
     * 插入提交记录
     */
    int insert(Submission submission);
    
    /**
     * 根据ID查询提交记录
     */
    Submission selectById(Long id);
    
    /**
     * 更新评测结果
     */
    int updateJudgeResult(Submission submission);
    
    /**
     * 查询用户的提交记录列表
     */
    List<Submission> selectByUserId(@Param("userId") Long userId, 
                                     @Param("offset") Integer offset, 
                                     @Param("limit") Integer limit);
    
    /**
     * 查询题目的提交记录列表
     */
    List<Submission> selectByProblemId(@Param("problemId") Long problemId, 
                                        @Param("offset") Integer offset, 
                                        @Param("limit") Integer limit);
    
    /**
     * 查询所有提交记录（分页）
     */
    List<Submission> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 统计用户提交次数
     */
    Integer countByUserId(Long userId);
    
    /**
     * 统计用户通过的提交次数
     */
    Integer countAcceptedByUserId(Long userId);
    
    /**
     * 统计用户解决的不同题目数（通过的题目去重）
     */
    Integer countDistinctAcceptedProblemsByUserId(Long userId);
}
