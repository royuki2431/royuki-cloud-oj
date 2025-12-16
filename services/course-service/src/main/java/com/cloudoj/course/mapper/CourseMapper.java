package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程 Mapper 接口
 */
public interface CourseMapper {
    
    /**
     * 根据主键查询
     */
    Course selectByPrimaryKey(Long id);
    
    /**
     * 根据教师ID查询课程列表
     */
    List<Course> selectByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据学生ID查询课程列表（通过班级关联）
     */
    List<Course> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 查询所有课程
     */
    List<Course> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 插入课程
     */
    int insert(Course record);
    
    /**
     * 选择性插入
     */
    int insertSelective(Course record);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(Course record);
    
    /**
     * 删除课程（逻辑删除，更新状态）
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
     * 查询所有课程（不分页）
     */
    List<Course> selectAllCourses();
}
