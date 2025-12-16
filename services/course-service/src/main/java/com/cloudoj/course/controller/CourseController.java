package com.cloudoj.course.controller;

import com.cloudoj.course.entity.Course;
import com.cloudoj.course.entity.CourseClass;
import com.cloudoj.course.service.ClassService;
import com.cloudoj.course.service.ClassStatisticsService;
import com.cloudoj.course.service.CourseService;
import com.cloudoj.course.service.HomeworkService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/course")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private ClassService classService;
    
    @Autowired
    private HomeworkService homeworkService;
    
    @Autowired
    private ClassStatisticsService classStatisticsService;
    
    @Autowired
    private com.cloudoj.course.mapper.HomeworkSubmissionMapper homeworkSubmissionMapper;
    
    @Autowired
    private com.cloudoj.course.mapper.HomeworkProblemMapper homeworkProblemMapper;
    
    @Autowired
    private com.cloudoj.course.mapper.HomeworkMapper homeworkMapper;
    
    @Autowired
    private com.cloudoj.course.feign.ProblemServiceClient problemServiceClient;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Course Service is running!";
    }
    
    /**
     * 同步所有班级和课程的学生数量
     */
    @PostMapping("/sync/studentCounts")
    public Result<Void> syncStudentCounts() {
        log.info("手动触发同步学生数量");
        classService.syncAllStudentCounts();
        return Result.success("同步完成", null);
    }
    
    // ==================== 课程管理 ====================
    
    /**
     * 创建课程
     */
    @PostMapping("/create")
    public Result<Long> createCourse(@RequestBody Course course) {
        log.info("创建课程：name={}, teacherId={}", course.getName(), course.getTeacherId());
        Long courseId = courseService.createCourse(course);
        return Result.success("创建成功", courseId);
    }
    
    /**
     * 查询课程详情
     */
    @GetMapping("/detail/{id}")
    public Result<Course> getCourseDetail(@PathVariable Long id) {
        log.info("查询课程详情：id={}", id);
        Course course = courseService.getCourseById(id);
        return Result.success(course);
    }
    
    /**
     * 根据ID查询课程（简化路径）
     */
    @GetMapping("/{id}")
    public Result<Course> getCourseById(@PathVariable Long id) {
        log.info("查询课程：id={}", id);
        Course course = courseService.getCourseById(id);
        return Result.success(course);
    }
    
    /**
     * 查询教师的课程列表
     */
    @GetMapping("/teacher/{teacherId}")
    public Result<List<Course>> getTeacherCourses(@PathVariable Long teacherId) {
        log.info("查询教师课程列表：teacherId={}", teacherId);
        List<Course> courses = courseService.getTeacherCourses(teacherId);
        return Result.success(courses);
    }
    
    /**
     * 查询学生的课程列表
     */
    @GetMapping("/student/{studentId}")
    public Result<List<Course>> getStudentCourses(@PathVariable Long studentId) {
        log.info("查询学生课程列表：studentId={}", studentId);
        List<Course> courses = courseService.getStudentCourses(studentId);
        return Result.success(courses);
    }
    
    /**
     * 更新课程信息
     */
    @PutMapping("/update")
    public Result<Void> updateCourse(@RequestBody Course course) {
        log.info("更新课程：id={}", course.getId());
        courseService.updateCourse(course);
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除课程
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        log.info("删除课程：id={}", id);
        courseService.deleteCourse(id);
        return Result.success("删除成功", null);
    }
    
    // ==================== 班级管理 ====================
    
    /**
     * 创建班级
     */
    @PostMapping("/class/create")
    public Result<Map<String, Object>> createClass(@RequestBody CourseClass courseClass) {
        log.info("创建班级：name={}, courseId={}", courseClass.getName(), courseClass.getCourseId());
        Long classId = classService.createClass(courseClass);
        
        // 返回班级ID和邀请码
        CourseClass created = classService.getClassById(classId);
        Map<String, Object> result = new HashMap<>();
        result.put("classId", classId);
        result.put("inviteCode", created.getCode());
        
        return Result.success("创建成功", result);
    }
    
    /**
     * 查询班级详情
     */
    @GetMapping("/class/detail/{id}")
    public Result<CourseClass> getClassDetail(@PathVariable Long id) {
        log.info("查询班级详情：id={}", id);
        CourseClass courseClass = classService.getClassById(id);
        return Result.success(courseClass);
    }
    
    /**
     * 查询课程的班级列表（带统计信息）
     */
    @GetMapping("/class/list/{courseId}")
    public Result<List<Map<String, Object>>> getCourseClasses(@PathVariable Long courseId) {
        log.info("查询课程班级列表：courseId={}", courseId);
        List<CourseClass> classes = classService.getCourseClasses(courseId);
        
        // 为每个班级添加统计信息
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (CourseClass cc : classes) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", cc.getId());
            classInfo.put("courseId", cc.getCourseId());
            classInfo.put("name", cc.getName());
            classInfo.put("code", cc.getCode());
            classInfo.put("description", cc.getDescription());
            classInfo.put("teacherId", cc.getTeacherId());
            classInfo.put("maxStudents", cc.getMaxStudents());
            classInfo.put("studentCount", cc.getStudentCount());
            classInfo.put("status", cc.getStatus());
            classInfo.put("createdTime", cc.getCreatedTime());
            
            // 获取班级统计信息
            try {
                Map<String, Object> stats = classStatisticsService.getClassStatistics(cc.getId());
                classInfo.put("completionRate", parseCompletionRate(stats.get("completionRate")));
                classInfo.put("avgScore", parseAvgScore(stats.get("averageScore")));
            } catch (Exception e) {
                classInfo.put("completionRate", 0);
                classInfo.put("avgScore", 0.0);
            }
            
            result.add(classInfo);
        }
        
        return Result.success(result);
    }
    
    // 解析完成率字符串为数字
    private int parseCompletionRate(Object rate) {
        if (rate == null) return 0;
        String rateStr = rate.toString().replace("%", "");
        try {
            return (int) Double.parseDouble(rateStr);
        } catch (Exception e) {
            return 0;
        }
    }
    
    // 解析平均分字符串为数字
    private double parseAvgScore(Object score) {
        if (score == null) return 0.0;
        try {
            return Double.parseDouble(score.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 学生加入班级（使用邀请码）
     */
    @PostMapping("/class/join")
    public Result<Void> joinClass(@RequestParam Long studentId, 
                                   @RequestParam String inviteCode) {
        log.info("学生加入班级：studentId={}, inviteCode={}", studentId, inviteCode);
        classService.joinClass(studentId, inviteCode);
        return Result.success("加入成功", null);
    }
    
    /**
     * 更新班级信息
     */
    @PutMapping("/class/update")
    public Result<Void> updateClass(@RequestBody CourseClass courseClass) {
        log.info("更新班级：id={}", courseClass.getId());
        classService.updateClass(courseClass);
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除班级
     */
    @DeleteMapping("/class/delete/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        log.info("删除班级：id={}", id);
        classService.deleteClass(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 学生退出班级
     */
    @PostMapping("/class/leave")
    public Result<Void> leaveClass(@RequestParam Long studentId, @RequestParam Long classId) {
        log.info("学生退出班级：studentId={}, classId={}", studentId, classId);
        classService.leaveClass(studentId, classId);
        return Result.success("退出成功", null);
    }
    
    /**
     * 移除学生（教师操作）
     */
    @PostMapping("/class/removeStudent")
    public Result<Void> removeStudent(@RequestParam Long classId, @RequestParam Long studentId) {
        log.info("移除学生：classId={}, studentId={}", classId, studentId);
        classService.removeStudent(classId, studentId);
        return Result.success("移除成功", null);
    }
    
    /**
     * 获取班级学生列表
     */
    @GetMapping("/class/students/{classId}")
    public Result<List<Map<String, Object>>> getClassStudents(@PathVariable Long classId) {
        log.info("查询班级学生列表：classId={}", classId);
        List<Map<String, Object>> students = classService.getClassStudents(classId);
        return Result.success(students);
    }
    
    /**
     * 获取学生加入的班级列表
     */
    @GetMapping("/class/student/{studentId}")
    public Result<List<CourseClass>> getStudentClasses(@PathVariable Long studentId) {
        log.info("查询学生班级列表：studentId={}", studentId);
        List<CourseClass> classes = classService.getStudentClasses(studentId);
        return Result.success(classes);
    }
    
    /**
     * 获取学生加入的班级详细列表（包含课程和教师信息）
     */
    @GetMapping("/student/classes/{studentId}")
    public Result<List<Map<String, Object>>> getStudentClassesDetail(@PathVariable Long studentId) {
        log.info("查询学生班级详细列表：studentId={}", studentId);
        List<Map<String, Object>> classes = classService.getStudentClassesWithDetail(studentId);
        return Result.success(classes);
    }
    
    /**
     * 检查学生是否在班级中
     */
    @GetMapping("/class/check")
    public Result<Boolean> isStudentInClass(@RequestParam Long studentId, @RequestParam Long classId) {
        log.info("检查学生是否在班级中：studentId={}, classId={}", studentId, classId);
        boolean inClass = classService.isStudentInClass(studentId, classId);
        return Result.success(inClass);
    }
    
    // ==================== 作业管理 ====================
    
    /**
     * 创建作业
     */
    @PostMapping("/homework/create")
    public Result<Long> createHomework(@RequestBody Map<String, Object> request) {
        log.info("创建作业：{}", request);
        
        // 解析作业信息
        com.cloudoj.course.entity.Homework homework = new com.cloudoj.course.entity.Homework();
        homework.setCourseId(Long.valueOf(request.get("courseId").toString()));
        if (request.containsKey("classId")) {
            homework.setClassId(Long.valueOf(request.get("classId").toString()));
        }
        homework.setTitle((String) request.get("title"));
        homework.setDescription((String) request.get("description"));
        homework.setTeacherId(Long.valueOf(request.get("teacherId").toString()));
        
        // 解析时间（支持ISO 8601格式）
        if (request.containsKey("startTime") && request.get("startTime") != null) {
            homework.setStartTime(parseDateTime(request.get("startTime").toString()));
        }
        if (request.containsKey("endTime") && request.get("endTime") != null) {
            homework.setEndTime(parseDateTime(request.get("endTime").toString()));
        }
        
        // 解析题目列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> problemList = (List<Map<String, Object>>) request.get("problems");
        List<com.cloudoj.course.entity.HomeworkProblem> problems = new java.util.ArrayList<>();
        for (Map<String, Object> p : problemList) {
            com.cloudoj.course.entity.HomeworkProblem problem = new com.cloudoj.course.entity.HomeworkProblem();
            problem.setProblemId(Long.valueOf(p.get("problemId").toString()));
            problem.setScore(Integer.valueOf(p.get("score").toString()));
            problems.add(problem);
        }
        
        Long homeworkId = homeworkService.createHomework(homework, problems);
        return Result.success("创建成功", homeworkId);
    }
    
    /**
     * 查询作业详情
     */
    @GetMapping("/homework/detail/{id}")
    public Result<Map<String, Object>> getHomeworkDetail(@PathVariable Long id) {
        log.info("查询作业详情：id={}", id);
        Map<String, Object> detail = homeworkService.getHomeworkDetail(id);
        return Result.success(detail);
    }
    
    /**
     * 查询课程的作业列表（包含统计信息）
     */
    @GetMapping("/homework/course/{courseId}")
    public Result<List<Map<String, Object>>> getCourseHomeworks(@PathVariable Long courseId) {
        log.info("查询课程作业列表：courseId={}", courseId);
        List<Map<String, Object>> homeworks = homeworkService.getCourseHomeworksWithStats(courseId);
        return Result.success(homeworks);
    }
    
    /**
     * 查询班级的作业列表（包含题目数量）
     */
    @GetMapping("/homework/class/{classId}")
    public Result<List<Map<String, Object>>> getClassHomeworks(@PathVariable Long classId) {
        log.info("查询班级作业列表：classId={}", classId);
        List<Map<String, Object>> homeworks = homeworkService.getClassHomeworksWithProblemCount(classId);
        return Result.success(homeworks);
    }
    
    /**
     * 查询学生的作业列表
     */
    @GetMapping("/homework/student/{studentId}")
    public Result<List<Map<String, Object>>> getStudentHomeworks(@PathVariable Long studentId) {
        log.info("查询学生作业列表：studentId={}", studentId);
        List<Map<String, Object>> homeworks = homeworkService.getStudentHomeworks(studentId);
        return Result.success(homeworks);
    }
    
    /**
     * 查询学生的作业详情（包含题目完成情况）
     */
    @GetMapping("/homework/student/detail/{homeworkId}")
    public Result<Map<String, Object>> getStudentHomeworkDetail(
            @PathVariable Long homeworkId,
            @RequestParam Long studentId) {
        log.info("查询学生作业详情：homeworkId={}, studentId={}", homeworkId, studentId);
        Map<String, Object> detail = homeworkService.getStudentHomeworkDetail(homeworkId, studentId);
        return Result.success(detail);
    }
    
    /**
     * 更新作业信息（包含题目）
     */
    @PutMapping("/homework/update")
    public Result<Void> updateHomework(@RequestBody Map<String, Object> request) {
        Long homeworkId = Long.valueOf(request.get("id").toString());
        log.info("更新作业：id={}", homeworkId);
        
        // 更新作业基本信息
        com.cloudoj.course.entity.Homework homework = new com.cloudoj.course.entity.Homework();
        homework.setId(homeworkId);
        if (request.get("title") != null) {
            homework.setTitle(request.get("title").toString());
        }
        if (request.get("description") != null) {
            homework.setDescription(request.get("description").toString());
        }
        if (request.get("startTime") != null) {
            homework.setStartTime(parseDateTime(request.get("startTime").toString()));
        }
        if (request.get("endTime") != null) {
            homework.setEndTime(parseDateTime(request.get("endTime").toString()));
        }
        homeworkService.updateHomework(homework);
        
        // 更新题目列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> problemsData = (List<Map<String, Object>>) request.get("problems");
        if (problemsData != null) {
            List<com.cloudoj.course.entity.HomeworkProblem> problems = new java.util.ArrayList<>();
            for (Map<String, Object> p : problemsData) {
                com.cloudoj.course.entity.HomeworkProblem problem = new com.cloudoj.course.entity.HomeworkProblem();
                problem.setProblemId(Long.valueOf(p.get("problemId").toString()));
                problem.setScore(Integer.valueOf(p.get("score").toString()));
                problems.add(problem);
            }
            homeworkService.updateHomeworkProblems(homeworkId, problems);
        }
        
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除作业
     */
    @DeleteMapping("/homework/delete/{id}")
    public Result<Void> deleteHomework(@PathVariable Long id) {
        log.info("删除作业：id={}", id);
        homeworkService.deleteHomework(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 查询作业统计
     */
    @GetMapping("/homework/statistics/{homeworkId}")
    public Result<Map<String, Object>> getHomeworkStatistics(@PathVariable Long homeworkId) {
        log.info("查询作业统计：homeworkId={}", homeworkId);
        Map<String, Object> statistics = homeworkService.getHomeworkStatistics(homeworkId);
        return Result.success(statistics);
    }
    
    /**
     * 查询学生作业成绩
     */
    @GetMapping("/homework/score")
    public Result<Map<String, Object>> getStudentHomeworkScore(@RequestParam Long studentId,
                                                                @RequestParam Long homeworkId) {
        log.info("查询学生作业成绩：studentId={}, homeworkId={}", studentId, homeworkId);
        Map<String, Object> score = homeworkService.getStudentHomeworkScore(studentId, homeworkId);
        return Result.success(score);
    }
    
    /**
     * 记录作业提交（由judge-service调用）
     */
    @PostMapping("/homework/recordSubmission")
    public Result<Void> recordHomeworkSubmission(@RequestBody Map<String, Object> request) {
        Long homeworkId = Long.valueOf(request.get("homeworkId").toString());
        Long studentId = Long.valueOf(request.get("studentId").toString());
        Long problemId = Long.valueOf(request.get("problemId").toString());
        Long judgeSubmissionId = Long.valueOf(request.get("judgeSubmissionId").toString());
        Integer judgeScore = request.containsKey("score") ? Integer.valueOf(request.get("score").toString()) : 0;
        String status = (String) request.get("status");
        
        // 获取作业中该题目的分值设置
        List<com.cloudoj.course.entity.HomeworkProblem> homeworkProblems = homeworkProblemMapper.selectByHomeworkId(homeworkId);
        Integer problemMaxScore = 100; // 默认100分
        for (com.cloudoj.course.entity.HomeworkProblem hp : homeworkProblems) {
            if (hp.getProblemId().equals(problemId)) {
                problemMaxScore = hp.getScore() != null ? hp.getScore() : 100;
                break;
            }
        }
        
        // 按作业题目分值比例计算实际得分
        // judgeScore 是100分制，需要转换为作业题目分值
        // 例如：作业题目分值20分，评测得分80分（80%），实际得分 = 20 * 80 / 100 = 16分
        Integer actualScore = (int) Math.round((double) problemMaxScore * judgeScore / 100.0);
        
        log.info("记录作业提交：homeworkId={}, studentId={}, problemId={}, submissionId={}, judgeScore={}, problemMaxScore={}, actualScore={}, status={}", 
                homeworkId, studentId, problemId, judgeSubmissionId, judgeScore, problemMaxScore, actualScore, status);
        
        // 创建作业提交记录
        com.cloudoj.course.entity.HomeworkSubmission submission = new com.cloudoj.course.entity.HomeworkSubmission();
        submission.setHomeworkId(homeworkId);
        submission.setStudentId(studentId);
        submission.setProblemId(problemId);
        submission.setSubmissionId(judgeSubmissionId);
        submission.setScore(actualScore); // 使用按比例计算的实际得分
        submission.setStatus(status);
        submission.setSubmitTime(java.time.LocalDateTime.now());
        
        // 判断是否迟交
        com.cloudoj.course.entity.Homework homework = homeworkService.getHomeworkDetail(homeworkId) != null
                ? (com.cloudoj.course.entity.Homework) homeworkService.getHomeworkDetail(homeworkId).get("homework")
                : null;
        
        if (homework != null && homework.getEndTime() != null) {
            submission.setIsLate(submission.getSubmitTime().isAfter(homework.getEndTime()) ? 1 : 0);
        } else {
            submission.setIsLate(0);
        }
        
        // 保存提交记录
        homeworkSubmissionMapper.insertSelective(submission);
        
        log.info("作业提交记录成功，submissionId={}, actualScore={}", submission.getId(), actualScore);
        return Result.success("记录成功", null);
    }
    
    // ==================== 统计接口 ====================
    
    /**
     * 获取班级学情统计
     */
    @GetMapping("/statistics/class/{classId}")
    public Result<Map<String, Object>> getClassStatistics(@PathVariable Long classId) {
        log.info("查询班级统计：classId={}", classId);
        Map<String, Object> statistics = classStatisticsService.getClassStatistics(classId);
        return Result.success(statistics);
    }
    
    /**
     * 获取班级作业完成情况
     */
    @GetMapping("/statistics/homework")
    public Result<Map<String, Object>> getHomeworkCompletion(
            @RequestParam Long classId,
            @RequestParam Long homeworkId) {
        log.info("查询作业完成情况：classId={}, homeworkId={}", classId, homeworkId);
        Map<String, Object> completion = classStatisticsService.getHomeworkCompletion(classId, homeworkId);
        return Result.success(completion);
    }
    
    /**
     * 获取班级学生排名
     */
    @GetMapping("/statistics/ranking/{classId}")
    public Result<List<Map<String, Object>>> getStudentRanking(@PathVariable Long classId) {
        log.info("查询班级学生排名：classId={}", classId);
        List<Map<String, Object>> ranking = classStatisticsService.getStudentRanking(classId);
        return Result.success(ranking);
    }
    
    /**
     * 获取单个作业排名
     */
    @GetMapping("/statistics/homework/{homeworkId}/ranking")
    public Result<List<Map<String, Object>>> getHomeworkRanking(
            @PathVariable Long homeworkId,
            @RequestParam Long classId) {
        log.info("查询作业排名：homeworkId={}, classId={}", homeworkId, classId);
        List<Map<String, Object>> ranking = classStatisticsService.getHomeworkRanking(homeworkId, classId);
        return Result.success(ranking);
    }
    
    /**
     * 获取题库训练排名
     */
    @GetMapping("/statistics/practice/{classId}")
    public Result<List<Map<String, Object>>> getPracticeRanking(@PathVariable Long classId) {
        log.info("查询题库训练排名：classId={}", classId);
        List<Map<String, Object>> ranking = classStatisticsService.getPracticeRanking(classId);
        return Result.success(ranking);
    }
    
    /**
     * 获取课程整体统计
     */
    @GetMapping("/statistics/course/{courseId}")
    public Result<Map<String, Object>> getCourseStatistics(@PathVariable Long courseId) {
        log.info("查询课程统计：courseId={}", courseId);
        Map<String, Object> statistics = classStatisticsService.getCourseStatistics(courseId);
        return Result.success(statistics);
    }
    
    /**
     * 获取学生在班级的作业提交记录
     */
    @GetMapping("/homework/submissions")
    public Result<List<Map<String, Object>>> getStudentHomeworkSubmissions(
            @RequestParam Long classId,
            @RequestParam Long studentId) {
        log.info("查询学生作业提交记录：classId={}, studentId={}", classId, studentId);
        
        List<com.cloudoj.course.entity.HomeworkSubmission> submissions = 
            homeworkSubmissionMapper.selectByClassIdAndStudentId(classId, studentId);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.cloudoj.course.entity.HomeworkSubmission sub : submissions) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", sub.getId());
            item.put("homeworkId", sub.getHomeworkId());
            item.put("problemId", sub.getProblemId());
            item.put("score", sub.getScore());
            item.put("status", sub.getStatus());
            item.put("submitTime", sub.getSubmitTime());
            item.put("judgeSubmissionId", sub.getSubmissionId());
            
            // 获取作业标题
            com.cloudoj.course.entity.Homework homework = homeworkMapper.selectByPrimaryKey(sub.getHomeworkId());
            if (homework != null) {
                item.put("homeworkTitle", homework.getTitle());
            }
            
            // 获取题目标题和分值
            com.cloudoj.course.entity.HomeworkProblem hp = homeworkProblemMapper.selectByHomeworkIdAndProblemId(
                sub.getHomeworkId(), sub.getProblemId());
            if (hp != null) {
                item.put("maxScore", hp.getScore());
            }
            
            // 通过problemServiceClient获取题目标题
            try {
                Result<com.cloudoj.model.entity.problem.Problem> problemResult = 
                    problemServiceClient.getProblemById(sub.getProblemId());
                if (problemResult != null && problemResult.getCode() == 200 && problemResult.getData() != null) {
                    item.put("problemTitle", problemResult.getData().getTitle());
                } else {
                    item.put("problemTitle", "题目" + sub.getProblemId());
                }
            } catch (Exception e) {
                log.warn("获取题目信息失败: problemId={}", sub.getProblemId());
                item.put("problemTitle", "题目" + sub.getProblemId());
            }
            
            result.add(item);
        }
        
        return Result.success(result);
    }
    
    /**
     * 解析日期时间字符串（支持多种格式）
     */
    private java.time.LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析ISO 8601格式（带Z后缀）
            if (dateTimeStr.endsWith("Z")) {
                java.time.Instant instant = java.time.Instant.parse(dateTimeStr);
                return java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            }
            
            // 尝试解析带时区偏移的格式
            if (dateTimeStr.contains("+") || dateTimeStr.matches(".*-\\d{2}:\\d{2}$")) {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(dateTimeStr);
                return odt.toLocalDateTime();
            }
            
            // 尝试直接解析LocalDateTime格式
            return java.time.LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            log.warn("日期时间解析失败: {}, 尝试其他格式", dateTimeStr);
            // 尝试移除毫秒和Z后缀
            String cleaned = dateTimeStr.replaceAll("\\.\\d+Z?$", "").replace("Z", "");
            return java.time.LocalDateTime.parse(cleaned);
        }
    }
}
