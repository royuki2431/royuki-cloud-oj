package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.CourseClass;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班级 Mapper 接口
 */
public interface ClassMapper {
    
    /**
     * 根据主键查询
     */
    CourseClass selectByPrimaryKey(Long id);
    
    /**
     * 根据邀请码查询
     */
    CourseClass selectByCode(@Param("code") String code);
    
    /**
     * 根据课程ID查询班级列表
     */
    List<CourseClass> selectByCourseId(@Param("courseId") Long courseId);
    
    /**
     * 根据教师ID查询班级列表
     */
    List<CourseClass> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 插入班级
     */
    int insert(CourseClass record);
    
    /**
     * 选择性插入
     */
    int insertSelective(CourseClass record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(CourseClass record);
    
    /**
     * 删除班级
     */
    int deleteByPrimaryKey(Long id);
    
    /**
     * 更新学生人数
     */
    int updateStudentCount(@Param("id") Long id, @Param("increment") Integer increment);
    
    /**
     * 同步学生人数（根据实际关联数据更新）
     */
    int syncStudentCount(@Param("id") Long id, @Param("count") Integer count);
    
    /**
     * 查询所有班级
     */
    List<CourseClass> selectAll();
}
