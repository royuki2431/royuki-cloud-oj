package com.cloudoj.course.service.impl;

import com.cloudoj.course.entity.*;
import com.cloudoj.course.feign.ProblemServiceClient;
import com.cloudoj.course.feign.UserServiceClient;
import com.cloudoj.course.mapper.*;
import com.cloudoj.course.service.HomeworkService;
import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业服务实现类
 */
@Slf4j
@Service
public class HomeworkServiceImpl implements HomeworkService {
    
    @Autowired
    private HomeworkMapper homeworkMapper;
    
    @Autowired
    private HomeworkProblemMapper homeworkProblemMapper;
    
    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private ProblemServiceClient problemServiceClient;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHomework(Homework homework, List<HomeworkProblem> problems) {
        // 参数校验
        if (homework.getTitle() == null || homework.getTitle().trim().isEmpty()) {
            throw new RuntimeException("作业标题不能为空");
        }
        if (homework.getCourseId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }
        if (problems == null || problems.isEmpty()) {
            throw new RuntimeException("作业必须包含至少一道题目");
        }
        
        // 设置默认值
        if (homework.getStatus() == null) {
            homework.setStatus(1);
        }
        
        // 计算总分
        int totalScore = problems.stream()
                .mapToInt(p -> p.getScore() != null ? p.getScore() : 0)
                .sum();
        homework.setTotalScore(totalScore);
        
        // 插入作业
        homeworkMapper.insertSelective(homework);
        Long homeworkId = homework.getId();
        
        // 关联题目
        for (int i = 0; i < problems.size(); i++) {
            HomeworkProblem problem = problems.get(i);
            problem.setHomeworkId(homeworkId);
            problem.setOrderNum(i + 1);
        }
        homeworkProblemMapper.batchInsert(problems);
        
        log.info("创建作业成功，ID: {}, 题目数: {}, 总分: {}", homeworkId, problems.size(), totalScore);
        return homeworkId;
    }
    
    @Override
    public Map<String, Object> getHomeworkDetail(Long id) {
        Homework homework = homeworkMapper.selectByPrimaryKey(id);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        
        // 查询题目列表
        List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(id);
        
        // 获取题目详情（标题、难度）
        List<Map<String, Object>> problemDetails = new ArrayList<>();
        for (HomeworkProblem hp : problems) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("problemId", hp.getProblemId());
            detail.put("score", hp.getScore());
            detail.put("orderNum", hp.getOrderNum());
            
            // 调用problem-service获取题目信息
            try {
                Result<Problem> problemResult = problemServiceClient.getProblemById(hp.getProblemId());
                if (problemResult != null && problemResult.getCode() == 200 && problemResult.getData() != null) {
                    Problem problem = problemResult.getData();
                    detail.put("problemTitle", problem.getTitle());
                    detail.put("difficulty", problem.getDifficulty());
                } else {
                    detail.put("problemTitle", "题目#" + hp.getProblemId());
                    detail.put("difficulty", "MEDIUM");
                }
            } catch (Exception e) {
                log.warn("获取题目详情失败: problemId={}, error={}", hp.getProblemId(), e.getMessage());
                detail.put("problemTitle", "题目#" + hp.getProblemId());
                detail.put("difficulty", "MEDIUM");
            }
            
            problemDetails.add(detail);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("homework", homework);
        result.put("problems", problemDetails);
        
        return result;
    }
    
    @Override
    public List<Homework> getCourseHomeworks(Long courseId) {
        return homeworkMapper.selectByCourseId(courseId);
    }
    
    @Override
    public List<Map<String, Object>> getCourseHomeworksWithStats(Long courseId) {
        List<Homework> homeworks = homeworkMapper.selectByCourseId(courseId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Homework homework : homeworks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", homework.getId());
            item.put("title", homework.getTitle());
            item.put("description", homework.getDescription());
            item.put("courseId", homework.getCourseId());
            item.put("classId", homework.getClassId());
            item.put("teacherId", homework.getTeacherId());
            item.put("startTime", homework.getStartTime());
            item.put("endTime", homework.getEndTime());
            item.put("totalScore", homework.getTotalScore());
            item.put("createTime", homework.getCreatedTime());
            
            // 获取题目数量
            List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homework.getId());
            item.put("problemCount", problems.size());
            
            // 获取班级学生数量和完成情况
            int studentCount = 0;
            int completedCount = 0;
            
            if (homework.getClassId() != null) {
                List<StudentClass> students = studentClassMapper.selectByClassId(homework.getClassId());
                studentCount = (int) students.stream().filter(s -> s.getStatus() == 1).count();
                
                // 统计完成人数（提交了所有题目的学生）
                for (StudentClass sc : students) {
                    if (sc.getStatus() != 1) continue;
                    
                    int submittedCount = 0;
                    for (HomeworkProblem problem : problems) {
                        HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                                sc.getStudentId(), homework.getId(), problem.getProblemId());
                        if (best != null) {
                            submittedCount++;
                        }
                    }
                    if (submittedCount >= problems.size() && !problems.isEmpty()) {
                        completedCount++;
                    }
                }
            }
            
            item.put("studentCount", studentCount);
            item.put("completedCount", completedCount);
            item.put("completionRate", studentCount > 0 ? (completedCount * 100 / studentCount) : 0);
            
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    public List<Homework> getClassHomeworks(Long classId) {
        return homeworkMapper.selectByClassId(classId);
    }
    
    @Override
    public List<Map<String, Object>> getClassHomeworksWithProblemCount(Long classId) {
        List<Homework> homeworks = new ArrayList<>(homeworkMapper.selectByClassId(classId));
        
        // 同时获取课程级别的作业
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        if (courseClass != null) {
            List<Homework> courseHomeworks = homeworkMapper.selectByCourseId(courseClass.getCourseId());
            for (Homework hw : courseHomeworks) {
                if (hw.getClassId() == null) {
                    boolean exists = homeworks.stream().anyMatch(h -> h.getId().equals(hw.getId()));
                    if (!exists) {
                        homeworks.add(hw);
                    }
                }
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Homework hw : homeworks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", hw.getId());
            item.put("title", hw.getTitle());
            item.put("description", hw.getDescription());
            item.put("startTime", hw.getStartTime());
            item.put("endTime", hw.getEndTime());
            item.put("totalScore", hw.getTotalScore());
            item.put("classId", hw.getClassId());
            item.put("courseId", hw.getCourseId());
            
            // 查询题目数量
            List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(hw.getId());
            item.put("problemCount", problems.size());
            
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getStudentHomeworks(Long studentId) {
        // 查询学生加入的班级
        List<StudentClass> studentClasses = studentClassMapper.selectByStudentId(studentId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentClass sc : studentClasses) {
            if (sc.getStatus() != 1) continue;
            
            // 获取班级信息
            com.cloudoj.course.entity.CourseClass courseClass = 
                classMapper.selectByPrimaryKey(sc.getClassId());
            if (courseClass == null) continue;
            
            // 获取课程信息
            com.cloudoj.course.entity.Course course = 
                courseMapper.selectByPrimaryKey(courseClass.getCourseId());
            
            // 查询每个班级的作业
            List<Homework> homeworks = homeworkMapper.selectByClassId(sc.getClassId());
            
            for (Homework homework : homeworks) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", homework.getId());
                item.put("title", homework.getTitle());
                item.put("description", homework.getDescription());
                item.put("startTime", homework.getStartTime());
                item.put("endTime", homework.getEndTime());
                item.put("totalScore", homework.getTotalScore());
                item.put("classId", sc.getClassId());
                item.put("className", courseClass.getName());
                item.put("courseId", courseClass.getCourseId());
                item.put("courseName", course != null ? course.getName() : "未知课程");
                
                // 查询题目数量
                List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homework.getId());
                item.put("problemCount", problems.size());
                
                // 查询学生的完成情况
                List<HomeworkSubmission> submissions = 
                    homeworkSubmissionMapper.selectByStudentIdAndHomeworkId(studentId, homework.getId());
                
                // 计算已完成的题目数和得分
                Set<Long> completedProblemIds = submissions.stream()
                        .filter(s -> "ACCEPTED".equals(s.getStatus()) || s.getScore() != null)
                        .map(HomeworkSubmission::getProblemId)
                        .collect(Collectors.toSet());
                
                int earnedScore = 0;
                LocalDateTime lastSubmitTime = null;
                for (HomeworkProblem problem : problems) {
                    HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                            studentId, homework.getId(), problem.getProblemId());
                    if (best != null && best.getScore() != null) {
                        earnedScore += best.getScore();
                    }
                    if (best != null && best.getSubmitTime() != null) {
                        if (lastSubmitTime == null || best.getSubmitTime().isAfter(lastSubmitTime)) {
                            lastSubmitTime = best.getSubmitTime();
                        }
                    }
                }
                
                item.put("completedCount", completedProblemIds.size());
                item.put("earnedScore", earnedScore > 0 ? earnedScore : null);
                item.put("isCompleted", completedProblemIds.size() >= problems.size() && !problems.isEmpty());
                item.put("lastSubmitTime", lastSubmitTime);
                
                result.add(item);
            }
        }
        
        // 按截止时间排序，未截止的在前
        result.sort((a, b) -> {
            java.time.LocalDateTime endA = (java.time.LocalDateTime) a.get("endTime");
            java.time.LocalDateTime endB = (java.time.LocalDateTime) b.get("endTime");
            if (endA == null) return 1;
            if (endB == null) return -1;
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean overdueA = endA.isBefore(now);
            boolean overdueB = endB.isBefore(now);
            if (overdueA != overdueB) return overdueA ? 1 : -1;
            return endA.compareTo(endB);
        });
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHomework(Homework homework) {
        if (homework.getId() == null) {
            throw new RuntimeException("作业ID不能为空");
        }
        
        Homework existHomework = homeworkMapper.selectByPrimaryKey(homework.getId());
        if (existHomework == null) {
            throw new RuntimeException("作业不存在");
        }
        
        homeworkMapper.updateByPrimaryKeySelective(homework);
        log.info("更新作业成功，ID: {}", homework.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHomeworkProblems(Long homeworkId, List<HomeworkProblem> problems) {
        // 删除旧的题目
        homeworkProblemMapper.deleteByHomeworkId(homeworkId);
        
        // 插入新的题目
        if (problems != null && !problems.isEmpty()) {
            for (int i = 0; i < problems.size(); i++) {
                HomeworkProblem problem = problems.get(i);
                problem.setHomeworkId(homeworkId);
                problem.setOrderNum(i + 1);
            }
            homeworkProblemMapper.batchInsert(problems);
            
            // 更新总分
            int totalScore = problems.stream()
                    .mapToInt(p -> p.getScore() != null ? p.getScore() : 0)
                    .sum();
            
            Homework homework = new Homework();
            homework.setId(homeworkId);
            homework.setTotalScore(totalScore);
            homeworkMapper.updateByPrimaryKeySelective(homework);
        }
        
        log.info("更新作业题目成功，homeworkId: {}, 题目数: {}", homeworkId, problems.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHomework(Long id) {
        Homework existHomework = homeworkMapper.selectByPrimaryKey(id);
        if (existHomework == null) {
            throw new RuntimeException("作业不存在");
        }
        
        // 删除作业关联的题目
        homeworkProblemMapper.deleteByHomeworkId(id);
        
        // 删除作业
        homeworkMapper.deleteByPrimaryKey(id);
        
        log.info("删除作业成功，ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSubmission(Long studentId, Long problemId, Long judgeSubmissionId) {
        // 查找该题目属于哪个作业
        // 这里需要从题目ID反查作业，暂时简化处理
        // 实际应该在judge-service提交时就知道homeworkId
        log.info("记录作业提交：studentId={}, problemId={}, judgeSubmissionId={}", 
                studentId, problemId, judgeSubmissionId);
        
        // TODO: 完善提交记录逻辑
    }
    
    @Override
    public Map<String, Object> getHomeworkStatistics(Long homeworkId) {
        Homework homework = homeworkMapper.selectByPrimaryKey(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        
        // 查询题目列表
        List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homeworkId);
        
        // 获取班级学生列表
        List<StudentClass> students = new ArrayList<>();
        if (homework.getClassId() != null) {
            students = studentClassMapper.selectByClassId(homework.getClassId());
        }
        
        // 统计每个学生的完成情况
        List<Map<String, Object>> studentScores = new ArrayList<>();
        int completedCount = 0;
        
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue; // 跳过非正常状态的学生
            
            Map<String, Object> studentScore = new HashMap<>();
            studentScore.put("studentId", sc.getStudentId());
            
            // 获取学生信息（优先使用真实姓名）
            try {
                com.cloudoj.model.common.Result<Map<String, Object>> userResult = 
                    userServiceClient.getUserInfo(sc.getStudentId());
                if (userResult != null && userResult.getCode() == 200 && userResult.getData() != null) {
                    Map<String, Object> userInfo = userResult.getData();
                    String studentName = getStudentRealName(userInfo, sc.getStudentId());
                    studentScore.put("studentName", studentName);
                    studentScore.put("username", userInfo.get("username"));
                } else {
                    studentScore.put("studentName", "学生" + sc.getStudentId());
                    studentScore.put("username", "-");
                }
            } catch (Exception e) {
                studentScore.put("studentName", "学生" + sc.getStudentId());
                studentScore.put("username", "-");
            }
            
            // 计算该学生的得分
            int totalScore = 0;
            int earnedScore = 0;
            int submittedCount = 0;
            
            for (HomeworkProblem problem : problems) {
                totalScore += problem.getScore() != null ? problem.getScore() : 0;
                HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                        sc.getStudentId(), homeworkId, problem.getProblemId());
                if (best != null) {
                    submittedCount++;
                    earnedScore += best.getScore() != null ? best.getScore() : 0;
                }
            }
            
            studentScore.put("totalScore", totalScore);
            studentScore.put("earnedScore", earnedScore);
            studentScore.put("submittedCount", submittedCount);
            studentScore.put("problemCount", problems.size());
            studentScore.put("isCompleted", submittedCount >= problems.size() && !problems.isEmpty());
            
            if (submittedCount >= problems.size() && !problems.isEmpty()) {
                completedCount++;
            }
            
            studentScores.add(studentScore);
        }
        
        // 按得分降序排序
        studentScores.sort((a, b) -> {
            Integer scoreA = (Integer) a.get("earnedScore");
            Integer scoreB = (Integer) b.get("earnedScore");
            return scoreB.compareTo(scoreA);
        });
        
        // 统计每道题的完成情况
        List<Map<String, Object>> problemStats = new ArrayList<>();
        for (HomeworkProblem problem : problems) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("problemId", problem.getProblemId());
            stat.put("score", problem.getScore());
            stat.put("orderNum", problem.getOrderNum());
            
            // 获取题目标题
            try {
                com.cloudoj.model.common.Result<com.cloudoj.model.entity.problem.Problem> problemResult = 
                    problemServiceClient.getProblemById(problem.getProblemId());
                if (problemResult != null && problemResult.getCode() == 200 && problemResult.getData() != null) {
                    stat.put("problemTitle", problemResult.getData().getTitle());
                } else {
                    stat.put("problemTitle", "题目#" + problem.getProblemId());
                }
            } catch (Exception e) {
                stat.put("problemTitle", "题目#" + problem.getProblemId());
            }
            
            // 统计该题的提交人数和通过人数
            int submitCount = 0;
            int passCount = 0;
            for (StudentClass sc : students) {
                if (sc.getStatus() != 1) continue;
                HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                        sc.getStudentId(), homeworkId, problem.getProblemId());
                if (best != null) {
                    submitCount++;
                    if ("ACCEPTED".equals(best.getStatus())) {
                        passCount++;
                    }
                }
            }
            stat.put("submitCount", submitCount);
            stat.put("passCount", passCount);
            stat.put("studentCount", students.stream().filter(s -> s.getStatus() == 1).count());
            
            problemStats.add(stat);
        }
        
        // 计算平均分和通过率
        long activeStudentCount = students.stream().filter(s -> s.getStatus() == 1).count();
        double avgScore = 0;
        double passRate = 0;
        
        if (!studentScores.isEmpty()) {
            int totalEarned = 0;
            int passedStudents = 0;
            int maxScore = problems.stream().mapToInt(p -> p.getScore() != null ? p.getScore() : 0).sum();
            
            for (Map<String, Object> ss : studentScores) {
                int earned = (Integer) ss.get("earnedScore");
                totalEarned += earned;
                // 得分率>=60%算通过
                if (maxScore > 0 && earned >= maxScore * 0.6) {
                    passedStudents++;
                }
            }
            
            avgScore = (double) totalEarned / studentScores.size();
            passRate = activeStudentCount > 0 ? (double) passedStudents / activeStudentCount * 100 : 0;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("homework", homework);
        result.put("problemCount", problems.size());
        result.put("studentCount", activeStudentCount);
        result.put("completedCount", completedCount);
        result.put("avgScore", avgScore);
        result.put("passRate", passRate);
        result.put("studentScores", studentScores);
        result.put("problemStats", problemStats);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getStudentHomeworkScore(Long studentId, Long homeworkId) {
        // 查询作业题目
        List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homeworkId);
        
        // 查询学生每道题的最佳成绩
        List<Map<String, Object>> problemScores = new ArrayList<>();
        int totalScore = 0;
        int earnedScore = 0;
        
        for (HomeworkProblem problem : problems) {
            HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                    studentId, homeworkId, problem.getProblemId());
            
            Map<String, Object> item = new HashMap<>();
            item.put("problemId", problem.getProblemId());
            item.put("maxScore", problem.getScore());
            item.put("earnedScore", best != null ? best.getScore() : 0);
            item.put("status", best != null ? best.getStatus() : "NOT_SUBMITTED");
            
            problemScores.add(item);
            
            totalScore += problem.getScore();
            earnedScore += (best != null && best.getScore() != null) ? best.getScore() : 0;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("homeworkId", homeworkId);
        result.put("totalScore", totalScore);
        result.put("earnedScore", earnedScore);
        result.put("problemScores", problemScores);
        
        return result;
    }
    
    @Override
    public Map<String, Object> getStudentHomeworkDetail(Long homeworkId, Long studentId) {
        Homework homework = homeworkMapper.selectByPrimaryKey(homeworkId);
        if (homework == null) {
            throw new RuntimeException("作业不存在");
        }
        
        // 查询题目列表
        List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homeworkId);
        
        // 获取题目详情和学生完成情况
        List<Map<String, Object>> problemDetails = new ArrayList<>();
        for (HomeworkProblem hp : problems) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("problemId", hp.getProblemId());
            detail.put("score", hp.getScore());
            detail.put("orderNum", hp.getOrderNum());
            
            // 调用problem-service获取题目信息
            try {
                Result<Problem> problemResult = problemServiceClient.getProblemById(hp.getProblemId());
                if (problemResult != null && problemResult.getCode() == 200 && problemResult.getData() != null) {
                    Problem problem = problemResult.getData();
                    detail.put("problemTitle", problem.getTitle());
                    detail.put("difficulty", problem.getDifficulty());
                } else {
                    detail.put("problemTitle", "题目#" + hp.getProblemId());
                    detail.put("difficulty", "MEDIUM");
                }
            } catch (Exception e) {
                log.warn("获取题目详情失败: problemId={}, error={}", hp.getProblemId(), e.getMessage());
                detail.put("problemTitle", "题目#" + hp.getProblemId());
                detail.put("difficulty", "MEDIUM");
            }
            
            // 查询学生该题的提交情况
            HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                    studentId, homeworkId, hp.getProblemId());
            
            detail.put("submitted", best != null);
            detail.put("earnedScore", best != null ? best.getScore() : null);
            detail.put("status", best != null ? best.getStatus() : null);
            
            problemDetails.add(detail);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("homework", homework);
        result.put("problems", problemDetails);
        
        return result;
    }
    
    /**
     * 获取学生真实姓名（优先使用realName，其次nickname，最后username）
     */
    private String getStudentRealName(Map<String, Object> userInfo, Long studentId) {
        if (userInfo.get("realName") != null && !userInfo.get("realName").toString().isEmpty()) {
            return userInfo.get("realName").toString();
        } else if (userInfo.get("nickname") != null && !userInfo.get("nickname").toString().isEmpty()) {
            return userInfo.get("nickname").toString();
        } else if (userInfo.get("username") != null) {
            return userInfo.get("username").toString();
        }
        return "学生" + studentId;
    }
}
