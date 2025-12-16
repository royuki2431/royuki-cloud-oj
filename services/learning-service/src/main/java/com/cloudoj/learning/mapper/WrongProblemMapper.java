package com.cloudoj.learning.mapper;

import com.cloudoj.learning.entity.WrongProblem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 错题记录 Mapper 接口
 */
public interface WrongProblemMapper {
    
    /**
     * 根据主键查询
     */
    WrongProblem selectByPrimaryKey(Long id);
    
    /**
     * 根据用户ID和题目ID查询
     */
    WrongProblem selectByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 根据用户ID查询错题列表
     */
    List<WrongProblem> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查询未解决的错题
     */
    List<WrongProblem> selectUnresolvedByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和错误类型查询
     */
    List<WrongProblem> selectByUserAndErrorType(@Param("userId") Long userId, @Param("errorType") String errorType);
    
    /**
     * 删除错题记录
     */
    int deleteByPrimaryKey(Long id);
    
    /**
     * 插入错题记录
     */
    int insert(WrongProblem record);
    
    /**
     * 选择性插入
     */
    int insertSelective(WrongProblem record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(WrongProblem record);
    
    /**
     * 更新错误次数
     */
    int incrementWrongCount(@Param("id") Long id);
    
    /**
     * 标记为已解决
     */
    int markAsResolved(@Param("id") Long id);
    
    /**
     * 统计用户错题数
     */
    Long countByUserId(@Param("userId") Long userId, @Param("isResolved") Integer isResolved);
}
