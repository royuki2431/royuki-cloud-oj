package com.cloudoj.course.service.impl;

import com.cloudoj.course.entity.CourseClass;
import com.cloudoj.course.entity.Homework;
import com.cloudoj.course.entity.HomeworkProblem;
import com.cloudoj.course.entity.HomeworkSubmission;
import com.cloudoj.course.entity.StudentClass;
import com.cloudoj.course.feign.UserServiceClient;
import com.cloudoj.course.mapper.ClassMapper;
import com.cloudoj.course.mapper.HomeworkMapper;
import com.cloudoj.course.mapper.HomeworkProblemMapper;
import com.cloudoj.course.mapper.HomeworkSubmissionMapper;
import com.cloudoj.course.mapper.StudentClassMapper;
import com.cloudoj.course.service.ClassStatisticsService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 班级统计服务实现类
 */
@Slf4j
@Service
public class ClassStatisticsServiceImpl implements ClassStatisticsService {
    
    @Autowired
    private ClassMapper classMapper;
    
    @Autowired
    private StudentClassMapper studentClassMapper;
    
    @Autowired
    private HomeworkMapper homeworkMapper;
    
    @Autowired
    private HomeworkProblemMapper homeworkProblemMapper;
    
    @Autowired
    private HomeworkSubmissionMapper homeworkSubmissionMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Override
    public Map<String, Object> getClassStatistics(Long classId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取班级信息
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        if (courseClass == null) {
            throw new RuntimeException("班级不存在");
        }
        
        stats.put("classId", classId);
        stats.put("className", courseClass.getName());
        
        // 获取班级学生列表（动态计算学生数量）
        List<StudentClass> students = studentClassMapper.selectByClassId(classId);
        int studentCount = (int) students.stream().filter(s -> s.getStatus() == 1).count();
        stats.put("studentCount", studentCount);
        stats.put("activeStudents", studentCount);
        
        // 获取班级作业列表（包括班级专属作业和课程级别作业）
        List<Homework> homeworks = homeworkMapper.selectByClassId(classId);
        // 同时获取课程级别的作业（class_id为null的作业）
        List<Homework> courseHomeworks = homeworkMapper.selectByCourseId(courseClass.getCourseId());
        for (Homework hw : courseHomeworks) {
            if (hw.getClassId() == null && !homeworks.contains(hw)) {
                homeworks.add(hw);
            }
        }
        stats.put("homeworkCount", homeworks.size());
        
        // 计算作业完成率
        int totalSubmissions = 0;
        int expectedSubmissions = students.size() * homeworks.size();
        
        for (Homework hw : homeworks) {
            List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectByHomeworkId(hw.getId());
            totalSubmissions += submissions.size();
        }
        
        double completionRate = expectedSubmissions > 0 
            ? (double) totalSubmissions / expectedSubmissions * 100 
            : 0;
        stats.put("completionRate", String.format("%.1f%%", completionRate));
        
        // 作业完成率 = 已完成作业的学生数 / (学生数 * 作业数)
        // 一个学生完成一个作业 = 该作业的所有题目都有提交
        int completedHomeworkCount = 0;
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue;
            for (Homework hw : homeworks) {
                List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(hw.getId());
                if (problems.isEmpty()) continue;
                
                boolean allCompleted = true;
                for (HomeworkProblem problem : problems) {
                    HomeworkSubmission sub = homeworkSubmissionMapper.selectBestSubmission(
                        sc.getStudentId(), hw.getId(), problem.getProblemId());
                    if (sub == null) {
                        allCompleted = false;
                        break;
                    }
                }
                if (allCompleted) {
                    completedHomeworkCount++;
                }
            }
        }
        int totalHomeworkAssignments = studentCount * homeworks.size();
        double homeworkCompletionRate = totalHomeworkAssignments > 0 
            ? (double) completedHomeworkCount / totalHomeworkAssignments * 100 
            : 0;
        stats.put("homeworkCompletionRate", String.format("%.1f%%", homeworkCompletionRate));
        
        // 计算平均分和提交统计
        List<HomeworkSubmission> allSubmissions = new ArrayList<>();
        for (Homework hw : homeworks) {
            allSubmissions.addAll(homeworkSubmissionMapper.selectByHomeworkId(hw.getId()));
        }
        
        // 班级平均分 = 班级所有学生总分之和 / 班级人数
        // 每个学生的总分 = 该学生所有作业得分之和
        double totalStudentScores = 0;
        int validStudentCount = 0;
        
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue;
            Long studentId = sc.getStudentId();
            
            // 计算该学生的总得分
            int studentTotalScore = 0;
            for (Homework hw : homeworks) {
                // 获取作业的题目列表
                List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(hw.getId());
                
                // 计算学生在该作业的得分（每道题取最高分）
                for (HomeworkProblem problem : problems) {
                    HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(studentId, hw.getId(), problem.getProblemId());
                    if (best != null && best.getScore() != null) {
                        studentTotalScore += best.getScore();
                    }
                }
            }
            
            totalStudentScores += studentTotalScore;
            validStudentCount++;
        }
        
        double avgScore = validStudentCount > 0 ? totalStudentScores / validStudentCount : 0;
        stats.put("averageScore", String.format("%.1f", avgScore));
        
        // 提交分析统计
        stats.put("totalSubmissions", allSubmissions.size());
        
        // 统计通过数和各状态数量
        int acceptedCount = 0;
        Map<String, Integer> statusCounts = new HashMap<>();
        for (HomeworkSubmission sub : allSubmissions) {
            String status = sub.getStatus();
            if ("ACCEPTED".equals(status)) {
                acceptedCount++;
            }
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }
        stats.put("acceptedCount", acceptedCount);
        stats.put("acceptRate", allSubmissions.size() > 0 ? (double) acceptedCount / allSubmissions.size() * 100 : 0);
        
        // 错误分析
        List<Map<String, Object>> errorAnalysis = new ArrayList<>();
        int totalErrors = allSubmissions.size() - acceptedCount;
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            if (!"ACCEPTED".equals(entry.getKey()) && entry.getValue() > 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("type", entry.getKey());
                error.put("count", entry.getValue());
                error.put("percentage", totalErrors > 0 ? (double) entry.getValue() / totalErrors * 100 : 0);
                errorAnalysis.add(error);
            }
        }
        // 按数量排序
        errorAnalysis.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        stats.put("errorAnalysis", errorAnalysis);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getHomeworkCompletion(Long classId, Long homeworkId) {
        Map<String, Object> completion = new HashMap<>();
        
        // 获取班级学生
        List<StudentClass> students = studentClassMapper.selectByClassId(classId);
        int totalStudents = students.size();
        
        // 获取作业提交
        List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectByHomeworkId(homeworkId);
        
        // 统计提交情况
        Set<Long> submittedStudents = new HashSet<>();
        int passCount = 0;
        int totalScore = 0;
        
        for (HomeworkSubmission sub : submissions) {
            submittedStudents.add(sub.getStudentId());
            if ("ACCEPTED".equals(sub.getStatus())) {
                passCount++;
            }
            totalScore += sub.getScore();
        }
        
        completion.put("totalStudents", totalStudents);
        completion.put("submittedCount", submittedStudents.size());
        completion.put("notSubmittedCount", totalStudents - submittedStudents.size());
        completion.put("passCount", passCount);
        completion.put("submissionRate", totalStudents > 0 
            ? String.format("%.1f%%", (double) submittedStudents.size() / totalStudents * 100) 
            : "0%");
        completion.put("averageScore", submissions.size() > 0 
            ? String.format("%.1f", (double) totalScore / submissions.size()) 
            : "0");
        
        return completion;
    }
    
    @Override
    public List<Map<String, Object>> getStudentRanking(Long classId) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        // 获取班级信息
        CourseClass courseClass = classMapper.selectByPrimaryKey(classId);
        
        // 获取班级学生
        List<StudentClass> students = studentClassMapper.selectByClassId(classId);
        
        // 获取班级作业（班级专属 + 课程级别）
        List<Homework> homeworks = homeworkMapper.selectByClassId(classId);
        if (courseClass != null) {
            List<Homework> courseHomeworks = homeworkMapper.selectByCourseId(courseClass.getCourseId());
            for (Homework hw : courseHomeworks) {
                if (hw.getClassId() == null && !homeworks.contains(hw)) {
                    homeworks.add(hw);
                }
            }
        }
        
        // 计算每个学生的总分
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue;
            
            Map<String, Object> studentRank = new HashMap<>();
            studentRank.put("studentId", sc.getStudentId());
            
            // 获取学生姓名
            try {
                Result<Map<String, Object>> userResult = userServiceClient.getUserInfo(sc.getStudentId());
                if (userResult != null && userResult.isSuccess() && userResult.getData() != null) {
                    Map<String, Object> userInfo = userResult.getData();
                    studentRank.put("studentName", getStudentRealName(userInfo, sc.getStudentId()));
                } else {
                    studentRank.put("studentName", "用户" + sc.getStudentId());
                }
            } catch (Exception e) {
                studentRank.put("studentName", "用户" + sc.getStudentId());
            }
            
            int totalScore = 0;
            int completedHomeworks = 0;
            
            for (Homework hw : homeworks) {
                List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(hw.getId());
                int homeworkScore = 0;
                boolean allCompleted = true;
                
                for (HomeworkProblem problem : problems) {
                    HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                        sc.getStudentId(), hw.getId(), problem.getProblemId());
                    if (best != null && best.getScore() != null) {
                        homeworkScore += best.getScore();
                    } else {
                        allCompleted = false;
                    }
                }
                
                totalScore += homeworkScore;
                if (allCompleted && !problems.isEmpty()) {
                    completedHomeworks++;
                }
            }
            
            studentRank.put("totalScore", totalScore);
            studentRank.put("completedHomeworks", completedHomeworks + "/" + homeworks.size());
            studentRank.put("averageScore", homeworks.size() > 0 
                ? String.format("%.1f", (double) totalScore / homeworks.size()) 
                : "0");
            
            ranking.add(studentRank);
        }
        
        // 按总分排序
        ranking.sort((a, b) -> (Integer) b.get("totalScore") - (Integer) a.get("totalScore"));
        
        // 添加排名
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }
        
        return ranking;
    }
    
    @Override
    public Map<String, Object> getCourseStatistics(Long courseId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取课程的所有班级
        List<CourseClass> classes = classMapper.selectByCourseId(courseId);
        
        int totalStudents = 0;
        int totalSubmissions = 0;
        double totalScore = 0;
        int scoreCount = 0;
        
        for (CourseClass cc : classes) {
            // 动态计算每个班级的学生数量
            Long classStudentCount = studentClassMapper.countByClassId(cc.getId());
            totalStudents += (classStudentCount != null ? classStudentCount.intValue() : 0);
        }
        
        // 获取课程的所有作业（包括课程级别和班级级别）
        List<Homework> allHomeworks = homeworkMapper.selectByCourseId(courseId);
        int totalHomeworks = allHomeworks.size();
        
        for (Homework hw : allHomeworks) {
            List<HomeworkSubmission> submissions = homeworkSubmissionMapper.selectByHomeworkId(hw.getId());
            totalSubmissions += submissions.size();
        }
        
        // 课程平均分 = 所有班级平均分之和 / 班级数
        double totalClassAvgScore = 0;
        int validClassCount = 0;
        
        for (CourseClass cc : classes) {
            // 获取班级学生
            List<StudentClass> classStudents = studentClassMapper.selectByClassId(cc.getId());
            List<StudentClass> activeStudents = classStudents.stream()
                .filter(s -> s.getStatus() == 1)
                .collect(java.util.stream.Collectors.toList());
            
            if (activeStudents.isEmpty()) continue;
            
            // 获取班级作业（班级专属 + 课程级别）
            List<Homework> classHomeworks = homeworkMapper.selectByClassId(cc.getId());
            for (Homework hw : allHomeworks) {
                if (hw.getClassId() == null && !classHomeworks.contains(hw)) {
                    classHomeworks.add(hw);
                }
            }
            
            // 计算班级平均分 = 班级所有学生总分之和 / 班级人数
            double classTotalScore = 0;
            for (StudentClass sc : activeStudents) {
                int studentTotalScore = 0;
                for (Homework hw : classHomeworks) {
                    List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(hw.getId());
                    for (HomeworkProblem problem : problems) {
                        HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                            sc.getStudentId(), hw.getId(), problem.getProblemId());
                        if (best != null && best.getScore() != null) {
                            studentTotalScore += best.getScore();
                        }
                    }
                }
                classTotalScore += studentTotalScore;
            }
            
            double classAvgScore = classTotalScore / activeStudents.size();
            totalClassAvgScore += classAvgScore;
            validClassCount++;
        }
        
        // 课程平均分 = 所有班级平均分之和 / 班级数
        double avgScore = validClassCount > 0 ? totalClassAvgScore / validClassCount : 0;
        
        stats.put("classCount", classes.size());
        stats.put("studentCount", totalStudents);
        stats.put("totalStudents", totalStudents);
        stats.put("homeworkCount", totalHomeworks);
        stats.put("totalHomeworks", totalHomeworks);
        stats.put("totalSubmissions", totalSubmissions);
        stats.put("avgScore", avgScore);
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getHomeworkRanking(Long homeworkId, Long classId) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        // 获取班级学生
        List<StudentClass> students = studentClassMapper.selectByClassId(classId);
        
        // 获取作业题目
        List<HomeworkProblem> problems = homeworkProblemMapper.selectByHomeworkId(homeworkId);
        int totalPossibleScore = problems.stream()
            .mapToInt(p -> p.getScore() != null ? p.getScore() : 0)
            .sum();
        
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue;
            
            Map<String, Object> studentRank = new HashMap<>();
            studentRank.put("studentId", sc.getStudentId());
            
            // 获取学生姓名
            try {
                Result<Map<String, Object>> userResult = userServiceClient.getUserInfo(sc.getStudentId());
                if (userResult != null && userResult.isSuccess() && userResult.getData() != null) {
                    Map<String, Object> userInfo = userResult.getData();
                    studentRank.put("studentName", getStudentRealName(userInfo, sc.getStudentId()));
                } else {
                    studentRank.put("studentName", "用户" + sc.getStudentId());
                }
            } catch (Exception e) {
                studentRank.put("studentName", "用户" + sc.getStudentId());
            }
            
            int score = 0;
            boolean completed = true;
            java.time.LocalDateTime lastSubmitTime = null;
            
            for (HomeworkProblem problem : problems) {
                HomeworkSubmission best = homeworkSubmissionMapper.selectBestSubmission(
                    sc.getStudentId(), homeworkId, problem.getProblemId());
                if (best != null) {
                    if (best.getScore() != null) {
                        score += best.getScore();
                    }
                    if (best.getSubmitTime() != null) {
                        if (lastSubmitTime == null || best.getSubmitTime().isAfter(lastSubmitTime)) {
                            lastSubmitTime = best.getSubmitTime();
                        }
                    }
                } else {
                    completed = false;
                }
            }
            
            studentRank.put("score", score);
            studentRank.put("completed", completed);
            studentRank.put("submitTime", lastSubmitTime);
            
            ranking.add(studentRank);
        }
        
        // 按得分排序
        ranking.sort((a, b) -> (Integer) b.get("score") - (Integer) a.get("score"));
        
        // 添加排名
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }
        
        return ranking;
    }
    
    @Override
    public List<Map<String, Object>> getPracticeRanking(Long classId) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        
        // 获取班级学生
        List<StudentClass> students = studentClassMapper.selectByClassId(classId);
        
        for (StudentClass sc : students) {
            if (sc.getStatus() != 1) continue;
            
            Map<String, Object> studentRank = new HashMap<>();
            studentRank.put("studentId", sc.getStudentId());
            
            // 获取学生姓名
            try {
                Result<Map<String, Object>> userResult = userServiceClient.getUserInfo(sc.getStudentId());
                if (userResult != null && userResult.isSuccess() && userResult.getData() != null) {
                    Map<String, Object> userInfo = userResult.getData();
                    studentRank.put("studentName", getStudentRealName(userInfo, sc.getStudentId()));
                } else {
                    studentRank.put("studentName", "用户" + sc.getStudentId());
                }
            } catch (Exception e) {
                studentRank.put("studentName", "用户" + sc.getStudentId());
            }
            
            // 获取学生在该班级的作业提交统计
            int solvedCount = 0;
            int submitCount = 0;
            int totalScore = 0;
            
            // 只统计该班级的作业提交
            List<HomeworkSubmission> classSubs = homeworkSubmissionMapper.selectByClassIdAndStudentId(classId, sc.getStudentId());
            Set<Long> solvedProblems = new HashSet<>();
            Map<Long, Integer> problemBestScore = new HashMap<>();
            
            for (HomeworkSubmission sub : classSubs) {
                submitCount++;
                if ("ACCEPTED".equals(sub.getStatus())) {
                    solvedProblems.add(sub.getProblemId());
                }
                // 记录每道题的最高分
                if (sub.getScore() != null) {
                    problemBestScore.merge(sub.getProblemId(), sub.getScore(), Math::max);
                }
            }
            solvedCount = solvedProblems.size();
            // 总分 = 该班级所有题目的最高分之和
            totalScore = problemBestScore.values().stream().mapToInt(Integer::intValue).sum();
            
            studentRank.put("solvedCount", solvedCount);
            studentRank.put("submitCount", submitCount);
            studentRank.put("totalScore", totalScore);
            studentRank.put("acceptRate", submitCount > 0 ? Math.round((double) solvedCount / submitCount * 100) : 0);
            
            ranking.add(studentRank);
        }
        
        // 按总分排序（总分相同按通过题数排序）
        ranking.sort((a, b) -> {
            int scoreCompare = (Integer) b.get("totalScore") - (Integer) a.get("totalScore");
            if (scoreCompare != 0) return scoreCompare;
            return (Integer) b.get("solvedCount") - (Integer) a.get("solvedCount");
        });
        
        // 添加排名
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }
        
        return ranking;
    }
    
    @Override
    public void recordDailyStatistics(Long classId) {
        // 可以在这里实现每日统计记录逻辑
        log.info("记录班级每日统计：classId={}", classId);
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
        return "用户" + studentId;
    }
}
