package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.HomeworkProblem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作业-题目关联 Mapper 接口
 */
public interface HomeworkProblemMapper {
    
    /**
     * 根据主键查询
     */
    HomeworkProblem selectByPrimaryKey(Long id);
    
    /**
     * 根据作业ID查询题目列表
     */
    List<HomeworkProblem> selectByHomeworkId(@Param("homeworkId") Long homeworkId);
    
    /**
     * 根据作业ID和题目ID查询
     */
    HomeworkProblem selectByHomeworkIdAndProblemId(@Param("homeworkId") Long homeworkId,
                                                    @Param("problemId") Long problemId);
    
    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<HomeworkProblem> list);
    
    /**
     * 插入
     */
    int insert(HomeworkProblem record);
    
    /**
     * 删除作业的所有题目
     */
    int deleteByHomeworkId(@Param("homeworkId") Long homeworkId);
}
