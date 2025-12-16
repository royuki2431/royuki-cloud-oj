package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.Homework;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作业 Mapper 接口
 */
public interface HomeworkMapper {
    
    /**
     * 根据主键查询
     */
    Homework selectByPrimaryKey(Long id);
    
    /**
     * 根据课程ID查询作业列表
     */
    List<Homework> selectByCourseId(@Param("courseId") Long courseId);
    
    /**
     * 根据班级ID查询作业列表
     */
    List<Homework> selectByClassId(@Param("classId") Long classId);
    
    /**
     * 根据教师ID查询作业列表
     */
    List<Homework> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 插入作业
     */
    int insert(Homework record);
    
    /**
     * 选择性插入
     */
    int insertSelective(Homework record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(Homework record);
    
    /**
     * 删除作业
     */
    int deleteByPrimaryKey(Long id);
}
