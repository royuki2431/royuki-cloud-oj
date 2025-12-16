package com.cloudoj.course.mapper;

import com.cloudoj.course.entity.HomeworkSubmission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作业提交 Mapper 接口
 */
public interface HomeworkSubmissionMapper {
    
    /**
     * 根据主键查询
     */
    HomeworkSubmission selectByPrimaryKey(Long id);
    
    /**
     * 根据作业ID查询提交列表
     */
    List<HomeworkSubmission> selectByHomeworkId(@Param("homeworkId") Long homeworkId);
    
    /**
     * 根据学生ID查询提交列表
     */
    List<HomeworkSubmission> selectByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据学生ID和作业ID查询提交列表
     */
    List<HomeworkSubmission> selectByStudentIdAndHomeworkId(@Param("studentId") Long studentId,
                                                             @Param("homeworkId") Long homeworkId);
    
    /**
     * 查询学生在某个题目上的最佳提交
     */
    HomeworkSubmission selectBestSubmission(@Param("studentId") Long studentId,
                                           @Param("homeworkId") Long homeworkId,
                                           @Param("problemId") Long problemId);
    
    /**
     * 根据作业ID和学生ID查询最佳提交
     */
    HomeworkSubmission selectByHomeworkAndStudent(@Param("homeworkId") Long homeworkId,
                                                   @Param("studentId") Long studentId);
    
    /**
     * 统计作业完成情况
     */
    Long countCompletedStudents(@Param("homeworkId") Long homeworkId);
    
    /**
     * 插入提交记录
     */
    int insert(HomeworkSubmission record);
    
    /**
     * 选择性插入
     */
    int insertSelective(HomeworkSubmission record);
    
    /**
     * 更新提交状态和得分
     */
    int updateScoreAndStatus(@Param("id") Long id,
                            @Param("score") Integer score,
                            @Param("status") String status);
    
    /**
     * 根据班级ID和学生ID查询提交列表
     */
    List<HomeworkSubmission> selectByClassIdAndStudentId(@Param("classId") Long classId,
                                                          @Param("studentId") Long studentId);
}
