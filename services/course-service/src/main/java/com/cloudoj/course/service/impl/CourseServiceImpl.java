package com.cloudoj.course.service.impl;

import com.cloudoj.course.entity.Course;
import com.cloudoj.course.entity.CourseClass;
import com.cloudoj.course.mapper.ClassMapper;
import com.cloudoj.course.mapper.CourseMapper;
import com.cloudoj.course.mapper.StudentClassMapper;
import com.cloudoj.course.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 课程服务实现类
 */
@Slf4j
@Service
public class CourseServiceImpl implements CourseService {
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(Course course) {
        // 参数校验
        if (course.getName() == null || course.getName().trim().isEmpty()) {
            throw new RuntimeException("课程名称不能为空");
        }
        if (course.getTeacherId() == null) {
            throw new RuntimeException("教师ID不能为空");
        }
        
        // 设置默认值
        if (course.getStatus() == null) {
            course.setStatus(1);
        }
        
        courseMapper.insertSelective(course);
        log.info("创建课程成功，ID: {}", course.getId());
        return course.getId();
    }
    
    @Override
    public Course getCourseById(Long id) {
        Course course = courseMapper.selectByPrimaryKey(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        return course;
    }
    
    @Override
    public List<Course> getTeacherCourses(Long teacherId) {
        List<Course> courses = courseMapper.selectByTeacherId(teacherId);
        // 动态计算每个课程的班级数和学生总数
        for (Course course : courses) {
            List<CourseClass> classes = classMapper.selectByCourseId(course.getId());
            int totalStudents = 0;
            for (CourseClass cc : classes) {
                Long count = studentClassMapper.countByClassId(cc.getId());
                totalStudents += (count != null ? count.intValue() : 0);
            }
            course.setStudentCount(totalStudents);
        }
        return courses;
    }
    
    @Override
    public List<Course> getStudentCourses(Long studentId) {
        return courseMapper.selectByStudentId(studentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(Course course) {
        if (course.getId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }
        
        Course existCourse = courseMapper.selectByPrimaryKey(course.getId());
        if (existCourse == null) {
            throw new RuntimeException("课程不存在");
        }
        
        courseMapper.updateByPrimaryKeySelective(course);
        log.info("更新课程成功，ID: {}", course.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        Course existCourse = courseMapper.selectByPrimaryKey(id);
        if (existCourse == null) {
            throw new RuntimeException("课程不存在");
        }
        
        courseMapper.deleteByPrimaryKey(id);
        log.info("删除课程成功，ID: {}", id);
    }
}
