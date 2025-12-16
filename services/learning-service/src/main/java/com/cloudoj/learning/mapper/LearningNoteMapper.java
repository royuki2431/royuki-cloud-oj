package com.cloudoj.learning.mapper;

import com.cloudoj.learning.entity.LearningNote;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学习笔记 Mapper 接口
 */
public interface LearningNoteMapper {
    
    /**
     * 根据主键查询
     */
    LearningNote selectByPrimaryKey(Long id);
    
    /**
     * 根据用户ID查询笔记列表
     */
    List<LearningNote> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和题目ID查询笔记
     */
    List<LearningNote> selectByUserAndProblem(@Param("userId") Long userId, @Param("problemId") Long problemId);
    
    /**
     * 根据题目ID查询公开笔记
     */
    List<LearningNote> selectPublicByProblemId(@Param("problemId") Long problemId);
    
    /**
     * 搜索笔记
     */
    List<LearningNote> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 插入笔记
     */
    int insert(LearningNote record);
    
    /**
     * 选择性插入
     */
    int insertSelective(LearningNote record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(LearningNote record);
    
    /**
     * 删除笔记
     */
    int deleteByPrimaryKey(Long id);
    
    /**
     * 统计用户笔记数
     */
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 查询所有公开笔记（分页）
     */
    List<LearningNote> selectAllPublicNotes(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 增加浏览次数
     */
    int incrementViewCount(@Param("id") Long id);
    
    /**
     * 管理员查询所有笔记（分页+筛选）
     */
    List<LearningNote> selectAllNotes(@Param("offset") int offset, @Param("limit") int limit, 
                                       @Param("keyword") String keyword, @Param("isPublic") Integer isPublic);
    
    /**
     * 管理员统计笔记总数
     */
    long countAllNotes(@Param("keyword") String keyword, @Param("isPublic") Integer isPublic);
    
    /**
     * 更新笔记公开状态
     */
    int updatePublicStatus(@Param("id") Long id, @Param("isPublic") Integer isPublic);
}
