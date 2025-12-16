package com.cloudoj.course.service.impl;

import com.cloudoj.course.entity.*;
import com.cloudoj.course.feign.UserServiceClient;
import com.cloudoj.course.mapper.*;
import com.cloudoj.course.service.ClassService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 班级服务实现类
 */
@Slf4j
@Service
public class ClassServiceImpl implements ClassService {
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private HomeworkMapper homeworkMapper;
    
    @Autowired
    private HomeworkProblemMapper homeworkProblemMapper;
    
    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClass(CourseClass courseClass) {
        // 参数校验
        if (courseClass.getCourseId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }
        if (courseClass.getName() == null || courseClass.getName().trim().isEmpty()) {
            throw new RuntimeException("班级名称不能为空");
        }
        if (courseClass.getTeacherId() == null) {
            throw new RuntimeException("教师ID不能为空");
        }
        
        // 设置默认值
        if (courseClass.getStatus() == null) {
            courseClass.setStatus(1);
        }
        if (courseClass.getMaxStudents() == null) {
            courseClass.setMaxStudents(100);
        }
        
        // 生成6位随机邀请码
        String inviteCode = generateInviteCode();
        // 确保邀请码唯一
        while (classMapper.selectByCode(inviteCode) != null) {
            inviteCode = generateInviteCode();
        }
        courseClass.setCode(inviteCode);
        
        classMapper.insertSelective(courseClass);
        log.info("创建班级成功，ID: {}, 邀请码: {}", courseClass.getId(), inviteCode);
        return courseClass.getId();
    }
    
    @Override
    public CourseClass getClassById(Long id) {
        CourseClass courseClass = classMapper.selectByPrimaryKey(id);
        if (courseClass == null) {
            throw new RuntimeException("班级不存在");
        }
        // 动态计算实际学生数量
        Long actualCount = studentClassMapper.countByClassId(id);
        courseClass.setStudentCount(actualCount != null ? actualCount.intValue() : 0);
        return courseClass;
    }
    
    @Override
    public CourseClass getClassByCode(String code) {
        CourseClass courseClass = classMapper.selectByCode(code);
        if (courseClass == null) {
            throw new RuntimeException("邀请码无效");
        }
        return courseClass;
    }
    
    @Override
    public List<CourseClass> getCourseClasses(Long courseId) {
        List<CourseClass> classes = classMapper.selectByCourseId(courseId);
        // 动态计算每个班级的实际学生数量
        for (CourseClass cc : classes) {
            Long actualCount = studentClassMapper.countByClassId(cc.getId());
            cc.setStudentCount(actualCount != null ? actualCount.intValue() : 0);
        }
        return classes;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinClass(Long studentId, String inviteCode) {
        // 验证邀请码
        CourseClass courseClass = classMapper.selectByCode(inviteCode);
        if (courseClass == null) {
            throw new RuntimeException("邀请码无效");
        }
        
        // 检查是否已加入
        StudentClass existing = studentClassMapper.selectByStudentIdAndClassId(studentId, courseClass.getId());
        if (existing != null && existing.getStatus() == 1) {
            throw new RuntimeException("您已加入该班级");
        }
        
        // 检查班级人数是否已满
        Long currentCount = studentClassMapper.countByClassId(courseClass.getId());
        if (currentCount >= courseClass.getMaxStudents()) {
            throw new RuntimeException("班级人数已满");
        }
        
        // 创建学生-班级关联
        StudentClass studentClass = new StudentClass();
        studentClass.setStudentId(studentId);
        studentClass.setClassId(courseClass.getId());
        studentClass.setJoinTime(LocalDateTime.now());
        studentClass.setStatus(1);
        studentClassMapper.insertSelective(studentClass);
        
        // 更新班级人数
        classMapper.updateStudentCount(courseClass.getId(), 1);
        
        // 更新课程人数
        courseMapper.updateStudentCount(courseClass.getCourseId(), 1);
        
        log.info("学生加入班级成功，studentId={}, classId={}", studentId, courseClass.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveClass(Long studentId, Long classId) {
        StudentClass studentClass = studentClassMapper.selectByStudentIdAndClassId(studentId, classId);
        if (studentClass == null || studentClass.getStatus() != 1) {
            throw new RuntimeException("您未加入该班级");
        }
        
        // 更新状态为退出
        studentClass.setStatus(0);
        studentClassMapper.updateByPrimaryKeySelective(studentClass);
        
        // 更新班级人数
        classMapper.updateStudentCount(classId, -1);
        
        // 获取班级信息并更新课程人数
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        if (courseClass != null) {
            courseMapper.updateStudentCount(courseClass.getCourseId(), -1);
        }
        
        log.info("学生退出班级成功，studentId={}, classId={}", studentId, classId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long classId, Long studentId) {
        StudentClass studentClass = studentClassMapper.selectByStudentIdAndClassId(studentId, classId);
        if (studentClass == null || studentClass.getStatus() != 1) {
            throw new RuntimeException("该学生不在班级中");
        }
        
        // 更新状态为移除
        studentClass.setStatus(0);
        studentClassMapper.updateByPrimaryKeySelective(studentClass);
        
        // 更新班级人数
        classMapper.updateStudentCount(classId, -1);
        
        // 获取班级信息并更新课程人数
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        if (courseClass != null) {
            courseMapper.updateStudentCount(courseClass.getCourseId(), -1);
        }
        
        log.info("移除学生成功，classId={}, studentId={}", classId, studentId);
    }
    
    @Override
    public List<Map<String, Object>> getClassStudents(Long classId) {
        List<StudentClass> studentClasses = studentClassMapper.selectByClassId(classId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取班级信息
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        
        // 获取班级的所有作业（包括班级专属作业和课程级别作业）
        List<Homework> homeworks = new ArrayList<>(homeworkMapper.selectByClassId(classId));
        if (courseClass != null) {
            List<Homework> courseHomeworks = homeworkMapper.selectByCourseId(courseClass.getCourseId());
            for (Homework hw : courseHomeworks) {
                if (hw.getClassId() == null) {
                    homeworks.add(hw);
                }
            }
        }
        
        for (StudentClass sc : studentClasses) {
            if (sc.getStatus() == 1) {
                Map<String, Object> student = new HashMap<>();
                student.put("studentId", sc.getStudentId());
                student.put("joinTime", sc.getJoinTime());
                
                // 获取学生详细信息
                try {
                    Result<Map<String, Object>> userResult = userServiceClient.getUserInfo(sc.getStudentId());
                    if (userResult != null && userResult.isSuccess() && userResult.getData() != null) {
                        Map<String, Object> userInfo = userResult.getData();
                        // 优先使用真实姓名，其次昵称，最后用户名
                        String studentName = null;
                        if (userInfo.get("realName") != null && !userInfo.get("realName").toString().isEmpty()) {
                            studentName = userInfo.get("realName").toString();
                        } else if (userInfo.get("nickname") != null && !userInfo.get("nickname").toString().isEmpty()) {
                            studentName = userInfo.get("nickname").toString();
                        } else if (userInfo.get("username") != null) {
                            studentName = userInfo.get("username").toString();
                        } else {
                            studentName = "用户" + sc.getStudentId();
                        }
                        student.put("studentName", studentName);
                        student.put("username", userInfo.get("username"));
                        student.put("studentNo", userInfo.get("studentNo"));
                        student.put("email", userInfo.get("email"));
                    } else {
                        student.put("studentName", "用户" + sc.getStudentId());
                        student.put("username", "-");
                        student.put("studentNo", "-");
                    }
                } catch (Exception e) {
                    log.warn("获取用户信息失败, studentId={}", sc.getStudentId(), e);
                    student.put("studentName", "用户" + sc.getStudentId());
                    student.put("username", "-");
                    student.put("studentNo", "-");
                }
                
                // 计算作业完成情况
                int totalHomework = homeworks.size();
                int completedCount = 0;
                int totalProblems = 0;
                int completedProblems = 0;
                int totalScore = 0;
                int earnedScore = 0;
                
                for (Homework homework : homeworks) {
                    List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homework.getId());
                    totalProblems += problems.size();
                    
                    int homeworkCompletedProblems = 0;
                    for (HomeworkProblem problem : problems) {
                        totalScore += problem.getScore() != null ? problem.getScore() : 0;
                        HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                                sc.getStudentId(), homework.getId(), problem.getProblemId());
                        if (best != null) {
                            completedProblems++;
                            homeworkCompletedProblems++;
                            earnedScore += best.getScore() != null ? best.getScore() : 0;
                        }
                    }
                    
                    // 如果该作业的所有题目都完成了，则认为作业完成
                    if (problems.size() > 0 && homeworkCompletedProblems == problems.size()) {
                        completedCount++;
                    }
                }
                
                // 完成率 = 已完成题目数 / 总题目数 * 100
                double completionRate = totalProblems > 0 ? (double) completedProblems / totalProblems * 100 : 0;
                // 平均分 = 获得分数 / 总分 * 100（转换为百分制）
                double avgScore = totalScore > 0 ? (double) earnedScore / totalScore * 100 : 0;
                
                student.put("completionRate", Math.round(completionRate));
                student.put("avgScore", Math.round(avgScore * 10) / 10.0);
                student.put("totalProblems", totalProblems);
                student.put("completedProblems", completedProblems);
                // 添加作业完成数量（前端需要的字段）
                student.put("totalHomework", totalHomework);
                student.put("completedCount", completedCount);
                
                result.add(student);
            }
        }
        
        return result;
    }
    
    @Override
    public List<CourseClass> getStudentClasses(Long studentId) {
        List<StudentClass> studentClasses = studentClassMapper.selectByStudentId(studentId);
        List<CourseClass> result = new ArrayList<>();
        
        for (StudentClass sc : studentClasses) {
            if (sc.getStatus() == 1) {
                CourseClass courseClass = classMapper.selectByPrimaryKey(sc.getClassId());
                if (courseClass != null) {
                    result.add(courseClass);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getStudentClassesWithDetail(Long studentId) {
        List<StudentClass> studentClasses = studentClassMapper.selectByStudentId(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (StudentClass sc : studentClasses) {
            if (sc.getStatus() == 1) {
                CourseClass courseClass = classMapper.selectByPrimaryKey(sc.getClassId());
                if (courseClass != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("classId", courseClass.getId());
                    item.put("className", courseClass.getName());
                    item.put("classCode", courseClass.getCode());
                    item.put("courseId", courseClass.getCourseId());
                    item.put("studentCount", courseClass.getStudentCount());
                    item.put("joinTime", sc.getJoinTime());
                    
                    // 获取课程信息
                    com.cloudoj.course.entity.Course course = courseMapper.selectByPrimaryKey(courseClass.getCourseId());
                    if (course != null) {
                        item.put("courseName", course.getName());
                        item.put("teacherId", course.getTeacherId());
                        
                        // 获取教师信息
                        try {
                            Result<Map<String, Object>> userResult = 
                                userServiceClient.getUserInfo(course.getTeacherId());
                            if (userResult != null && userResult.getCode() == 200 && userResult.getData() != null) {
                                Object realName = userResult.getData().get("realName");
                                item.put("teacherName", realName != null ? realName.toString() : "教师");
                            } else {
                                item.put("teacherName", "教师");
                            }
                        } catch (Exception e) {
                            log.warn("获取教师信息失败: teacherId={}", course.getTeacherId());
                            item.put("teacherName", "教师");
                        }
                    } else {
                        item.put("courseName", "未知课程");
                        item.put("teacherName", "教师");
                    }
                    
                    result.add(item);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public boolean isStudentInClass(Long studentId, Long classId) {
        StudentClass studentClass = studentClassMapper.selectByStudentIdAndClassId(studentId, classId);
        return studentClass != null && studentClass.getStatus() == 1;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClass(CourseClass courseClass) {
        if (courseClass.getId() == null) {
            throw new RuntimeException("班级ID不能为空");
        }
        
        CourseClass existClass = classMapper.selectByPrimaryKey(courseClass.getId());
        if (existClass == null) {
            throw new RuntimeException("班级不存在");
        }
        
        classMapper.updateByPrimaryKeySelective(courseClass);
        log.info("更新班级成功，ID: {}", courseClass.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long id) {
        CourseClass existClass = classMapper.selectByPrimaryKey(id);
        if (existClass == null) {
            throw new RuntimeException("班级不存在");
        }
        
        classMapper.deleteByPrimaryKey(id);
        log.info("删除班级成功，ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAllStudentCounts() {
        log.info("开始同步所有班级和课程的学生数量...");
        
        // 同步所有班级的学生数量
        List<CourseClass> allClasses = classMapper.selectAll();
        for (CourseClass cc : allClasses) {
            Long count = studentClassMapper.countByClassId(cc.getId());
            int studentCount = count != null ? count.intValue() : 0;
            classMapper.syncStudentCount(cc.getId(), studentCount);
            log.info("同步班级学生数量：classId={}, count={}", cc.getId(), studentCount);
        }
        
        // 同步所有课程的学生数量
        List<com.cloudoj.course.entity.Course> allCourses = courseMapper.selectAllCourses();
        for (com.cloudoj.course.entity.Course course : allCourses) {
            // 计算课程下所有班级的学生总数
            List<CourseClass> courseClasses = classMapper.selectByCourseId(course.getId());
            int totalStudents = 0;
            for (CourseClass cc : courseClasses) {
                Long count = studentClassMapper.countByClassId(cc.getId());
                totalStudents += (count != null ? count.intValue() : 0);
            }
            courseMapper.syncStudentCount(course.getId(), totalStudents);
            log.info("同步课程学生数量：courseId={}, count={}", course.getId(), totalStudents);
        }
        
        log.info("同步完成！");
    }
    
    /**
     * 生成6位随机邀请码（包含数字和大写字母）
     */
    private String generateInviteCode() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
