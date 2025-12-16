package com.cloudoj.learning.controller;

import com.cloudoj.learning.entity.LearningNote;
import com.cloudoj.learning.entity.LearningProgress;
import com.cloudoj.learning.entity.WrongProblem;
import com.cloudoj.learning.service.LearningNoteService;
import com.cloudoj.learning.service.LearningProgressService;
import com.cloudoj.learning.service.LearningStatisticsService;
import com.cloudoj.learning.service.WrongProblemService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/learning")
public class LearningController {
    
    @Autowired
    private LearningProgressService learningProgressService;
    
    @Autowired
    private WrongProblemService wrongProblemService;
    
    @Autowired
    private LearningNoteService learningNoteService;
    
    @Autowired
    private LearningStatisticsService learningStatisticsService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Learning Service is running!";
    }
    
    /**
     * 获取用户学习进度列表
     */
    @GetMapping("/progress")
    public Result<List<LearningProgress>> getProgressList(@RequestParam Long userId) {
        log.info("查询学习进度列表：userId={}", userId);
        List<LearningProgress> progressList = learningProgressService.getUserProgressList(userId);
        return Result.success(progressList);
    }
    
    /**
     * 获取用户学习统计摘要
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getProgressSummary(@RequestParam Long userId) {
        log.info("查询学习统计摘要：userId={}", userId);
        Map<String, Object> summary = learningProgressService.getProgressSummary(userId);
        return Result.success(summary);
    }
    
    /**
     * 获取用户指定状态的学习进度
     */
    @GetMapping("/progress/status")
    public Result<List<LearningProgress>> getProgressByStatus(
            @RequestParam Long userId,
            @RequestParam String status) {
        log.info("查询学习进度：userId={}, status={}", userId, status);
        List<LearningProgress> progressList = learningProgressService.getUserProgressByStatus(userId, status);
        return Result.success(progressList);
    }
    
    /**
     * 更新学习进度（内部接口，由judge-service调用）
     */
    @PostMapping("/progress/update")
    public Result<Void> updateProgress(
            @RequestParam Long userId,
            @RequestParam Long problemId,
            @RequestParam String status,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) Integer executionTime) {
        log.info("更新学习进度：userId={}, problemId={}, status={}, score={}, executionTime={}", 
                 userId, problemId, status, score, executionTime);
        learningProgressService.updateProgress(userId, problemId, status, score, executionTime);
        return Result.success("更新成功", null);
    }
    
    // ==================== 错题本接口 ====================
    
    /**
     * 获取用户错题列表
     */
    @GetMapping("/wrong")
    public Result<List<WrongProblem>> getWrongProblems(@RequestParam Long userId) {
        log.info("查询错题列表：userId={}", userId);
        List<WrongProblem> wrongProblems = wrongProblemService.getUserWrongProblems(userId);
        return Result.success(wrongProblems);
    }
    
    /**
     * 获取用户未解决的错题
     */
    @GetMapping("/wrong/unresolved")
    public Result<List<WrongProblem>> getUnresolvedWrongProblems(@RequestParam Long userId) {
        log.info("查询未解决错题：userId={}", userId);
        List<WrongProblem> wrongProblems = wrongProblemService.getUnresolvedWrongProblems(userId);
        return Result.success(wrongProblems);
    }
    
    /**
     * 添加错题记录（内部接口，由judge-service调用）
     */
    @PostMapping("/wrong/add")
    public Result<Void> addWrongProblem(
            @RequestParam Long userId,
            @RequestParam Long problemId,
            @RequestParam Long submissionId,
            @RequestParam String errorType) {
        log.info("添加错题记录：userId={}, problemId={}, errorType={}", userId, problemId, errorType);
        wrongProblemService.addWrongProblem(userId, problemId, submissionId, errorType);
        return Result.success("添加成功", null);
    }
    
    /**
     * 标记错题为已解决
     */
    @PostMapping("/wrong/resolve")
    public Result<Void> resolveWrongProblem(
            @RequestParam Long userId,
            @RequestParam Long problemId) {
        log.info("标记错题已解决：userId={}, problemId={}", userId, problemId);
        wrongProblemService.resolveWrongProblem(userId, problemId);
        return Result.success("标记成功", null);
    }
    
    /**
     * 删除错题记录
     */
    @DeleteMapping("/wrong/{id}")
    public Result<Void> deleteWrongProblem(@PathVariable Long id, @RequestParam Long userId) {
        log.info("删除错题记录：id={}, userId={}", id, userId);
        wrongProblemService.deleteWrongProblem(id, userId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取错题统计
     */
    @GetMapping("/wrong/statistics")
    public Result<Map<String, Object>> getWrongProblemStatistics(@RequestParam Long userId) {
        log.info("查询错题统计：userId={}", userId);
        Map<String, Object> statistics = wrongProblemService.getWrongProblemStatistics(userId);
        return Result.success(statistics);
    }
    
    // ==================== 学习笔记接口 ====================
    
    /**
     * 创建笔记
     */
    @PostMapping("/note")
    public Result<Long> createNote(@RequestBody LearningNote note) {
        log.info("创建笔记：userId={}, problemId={}", note.getUserId(), note.getProblemId());
        Long noteId = learningNoteService.createNote(note);
        return Result.success("创建成功", noteId);
    }
    
    /**
     * 更新笔记
     */
    @PutMapping("/note")
    public Result<Void> updateNote(@RequestBody LearningNote note) {
        log.info("更新笔记：id={}", note.getId());
        learningNoteService.updateNote(note);
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除笔记
     */
    @DeleteMapping("/note/{id}")
    public Result<Void> deleteNote(@PathVariable Long id, @RequestParam Long userId) {
        log.info("删除笔记：id={}, userId={}", id, userId);
        learningNoteService.deleteNote(id, userId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取笔记详情
     */
    @GetMapping("/note/{id}")
    public Result<LearningNote> getNoteById(@PathVariable Long id) {
        log.info("查询笔记详情：id={}", id);
        LearningNote note = learningNoteService.getNoteById(id);
        return Result.success(note);
    }
    
    /**
     * 获取用户的笔记列表
     */
    @GetMapping("/note/user")
    public Result<List<LearningNote>> getUserNotes(@RequestParam Long userId) {
        log.info("查询用户笔记列表：userId={}", userId);
        List<LearningNote> notes = learningNoteService.getUserNotes(userId);
        return Result.success(notes);
    }
    
    /**
     * 获取题目的笔记列表（用户自己的）
     */
    @GetMapping("/note/problem")
    public Result<List<LearningNote>> getProblemNotes(
            @RequestParam Long userId,
            @RequestParam Long problemId) {
        log.info("查询题目笔记：userId={}, problemId={}", userId, problemId);
        List<LearningNote> notes = learningNoteService.getProblemNotes(userId, problemId);
        return Result.success(notes);
    }
    
    /**
     * 获取公开的笔记列表（按题目）
     */
    @GetMapping("/note/public")
    public Result<List<LearningNote>> getPublicNotes(@RequestParam Long problemId) {
        log.info("查询公开笔记：problemId={}", problemId);
        List<LearningNote> notes = learningNoteService.getPublicNotes(problemId);
        return Result.success(notes);
    }
    
    /**
     * 获取所有公开笔记（笔记广场）
     */
    @GetMapping("/note/public/all")
    public Result<List<LearningNote>> getAllPublicNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("查询笔记广场：page={}, size={}", page, size);
        List<LearningNote> notes = learningNoteService.getAllPublicNotes(page, size);
        return Result.success(notes);
    }
    
    /**
     * 查看笔记详情（增加浏览次数）
     */
    @GetMapping("/note/view/{id}")
    public Result<LearningNote> viewNote(@PathVariable Long id) {
        log.info("查看笔记详情：id={}", id);
        // 增加浏览次数
        learningNoteService.incrementViewCount(id);
        LearningNote note = learningNoteService.getNoteById(id);
        return Result.success(note);
    }
    
    /**
     * 搜索笔记
     */
    @GetMapping("/note/search")
    public Result<List<LearningNote>> searchNotes(
            @RequestParam Long userId,
            @RequestParam String keyword) {
        log.info("搜索笔记：userId={}, keyword={}", userId, keyword);
        List<LearningNote> notes = learningNoteService.searchNotes(userId, keyword);
        return Result.success(notes);
    }
    
    // ==================== 学习统计接口 ====================
    
    /**
     * 获取学习热力图数据
     */
    @GetMapping("/statistics/heatmap")
    public Result<List<Map<String, Object>>> getHeatmapData(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("查询学习热力图：userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        List<Map<String, Object>> heatmapData = learningStatisticsService.getHeatmapData(userId, startDate, endDate);
        return Result.success(heatmapData);
    }
    
    /**
     * 获取学习总览
     */
    @GetMapping("/statistics/overview")
    public Result<Map<String, Object>> getLearningOverview(@RequestParam Long userId) {
        log.info("查询学习总览：userId={}", userId);
        Map<String, Object> overview = learningStatisticsService.getLearningOverview(userId);
        return Result.success(overview);
    }
    
    /**
     * 获取排行榜
     */
    @GetMapping("/statistics/leaderboard")
    public Result<List<Map<String, Object>>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("查询排行榜：limit={}", limit);
        List<Map<String, Object>> leaderboard = learningStatisticsService.getLeaderboard(limit);
        return Result.success(leaderboard);
    }
    
    /**
     * 记录学习统计（内部接口）
     */
    @PostMapping("/statistics/record")
    public Result<Void> recordStatistics(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int submitCount,
            @RequestParam(defaultValue = "0") int acceptCount,
            @RequestParam(defaultValue = "0") int problemSolved,
            @RequestParam(defaultValue = "0") int codeLines) {
        log.info("记录学习统计：userId={}", userId);
        learningStatisticsService.recordDailyStatistics(userId, submitCount, acceptCount, problemSolved, codeLines);
        return Result.success("记录成功", null);
    }
    
    // ==================== 管理员笔记管理接口 ====================
    
    /**
     * 管理员获取所有笔记列表（分页）
     */
    @GetMapping("/admin/note/list")
    public Result<Map<String, Object>> adminGetAllNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isPublic) {
        log.info("管理员查询笔记列表：page={}, size={}, keyword={}, isPublic={}", page, size, keyword, isPublic);
        List<LearningNote> notes = learningNoteService.getAllNotes(page, size, keyword, isPublic);
        long total = learningNoteService.countAllNotes(keyword, isPublic);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", notes);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }
    
    /**
     * 管理员删除笔记
     */
    @DeleteMapping("/admin/note/{id}")
    public Result<Void> adminDeleteNote(@PathVariable Long id) {
        log.info("管理员删除笔记：id={}", id);
        learningNoteService.adminDeleteNote(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 管理员更新笔记公开状态
     */
    @PutMapping("/admin/note/{id}/public")
    public Result<Void> updateNotePublicStatus(
            @PathVariable Long id,
            @RequestParam Integer isPublic) {
        log.info("管理员更新笔记公开状态：id={}, isPublic={}", id, isPublic);
        learningNoteService.updateNotePublicStatus(id, isPublic);
        return Result.success("更新成功", null);
    }
}
