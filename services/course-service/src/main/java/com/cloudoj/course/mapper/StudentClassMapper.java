package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.StudentClass;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生-班级关联 Mapper 接口
 */
public interface StudentClassMapper {
    
    /**
     * 根据主键查询
     */
    StudentClass selectByPrimaryKey(Long id);
    
    /**
     * 根据学生ID和班级ID查询
     */
    StudentClass selectByStudentIdAndClassId(@Param("studentId") Long studentId, 
                                             @Param("classId") Long classId);
    
    /**
     * 根据班级ID查询学生列表
     */
    List<StudentClass> selectByClassId(@Param("classId") Long classId);
    
    /**
     * 根据学生ID查询班级列表
     */
    List<StudentClass> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 插入关联
     */
    int insert(StudentClass record);
    
    /**
     * 选择性插入
     */
    int insertSelective(StudentClass record);
    
    /**
     * 更新状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 选择性更新
     */
    int updateByPrimaryKeySelective(StudentClass record);
    
    /**
     * 统计班级学生数
     */
    Long countByClassId(@Param("classId") Long classId);
}
