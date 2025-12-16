package com.cloudoj.problem.mapper;

import com.cloudoj.model.entity.problem.Problem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目 Mapper 接口
 */
public interface ProblemMapper {
    
    /**
     * 根据主键查询
     */
    Problem selectByPrimaryKey(Long id);
    
    /**
     * 查询所有题目（分页）
     */
    List<Problem> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 根据难度查询
     */
    List<Problem> selectByDifficulty(@Param("difficulty") String difficulty, 
                                     @Param("offset") Integer offset, 
                                     @Param("limit") Integer limit);
    
    /**
     * 根据标题模糊查询
     */
    List<Problem> selectByTitle(@Param("title") String title, 
                                @Param("offset") Integer offset, 
                                @Param("limit") Integer limit);
    
    /**
     * 插入（选择性）
     */
    int insertSelective(Problem record);
    
    /**
     * 插入（所有字段）
     */
    int insert(Problem record);
    
    /**
     * 根据主键更新（选择性）
     */
    int updateByPrimaryKeySelective(Problem record);
    
    /**
     * 根据主键更新（所有字段）
     */
    int updateByPrimaryKey(Problem record);
    
    /**
     * 根据主键删除
     */
    int deleteByPrimaryKey(Long id);
    
    /**
     * 统计总数
     */
    Long countAll();
    
    /**
     * 更新提交统计
     */
    int updateSubmitCount(@Param("id") Long id, @Param("isAccepted") Boolean isAccepted);
    
    // ==================== 管理员功能 ====================
    
    /**
     * 查询题目列表（管理员/教师）
     * @param authorId 作者ID，非空时只查询该作者的题目
     */
    List<Problem> selectProblemListAdmin(@Param("keyword") String keyword,
                                         @Param("difficulty") String difficulty,
                                         @Param("category") String category,
                                         @Param("status") Integer status,
                                         @Param("authorId") Long authorId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);
    
    /**
     * 统计题目数量
     * @param authorId 作者ID，非空时只统计该作者的题目
     */
    int countProblems(@Param("keyword") String keyword,
                      @Param("difficulty") String difficulty,
                      @Param("category") String category,
                      @Param("status") Integer status,
                      @Param("authorId") Long authorId);
    
    /**
     * 查询热门题目（按提交次数排序）
     */
    List<Problem> selectHotProblems(@Param("limit") int limit);
}
