package com.cloudoj.problem.controller;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import com.cloudoj.model.entity.problem.TestCase;
import com.cloudoj.problem.mapper.TestCaseMapper;
import com.cloudoj.problem.service.ProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题库服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/problem")
public class ProblemController {
    
    @Autowired
    ProblemService problemService;
    
    @Autowired
    TestCaseMapper testCaseMapper;
    
    @Autowired
    com.cloudoj.problem.service.ProblemCacheService problemCacheService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "problem-service");
        result.put("message", "Problem Service is running!");
        return result;
    }
    
    /**
     * 测试接口
     */
    @GetMapping("/test")
    public String test() {
        return "Problem Service Test OK!";
    }
    
    /**
     * 获取题目详情（使用缓存）
     */
    @GetMapping("/{id}")
    public Result<Problem> getProblemById(@PathVariable Long id) {
        log.info("查询题目详情：id={}", id);
        // 增加访问量
        problemCacheService.incrementViewCount(id);
        // 从缓存获取
        Problem problem = problemCacheService.getProblemById(id);
        return Result.success(problem);
    }
    
    /**
     * 获取热门题目列表
     */
    @GetMapping("/hot")
    public Result<List<Problem>> getHotProblems(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询热门题目：limit={}", limit);
        List<Problem> problems = problemCacheService.getHotProblems(limit);
        return Result.success(problems);
    }
    
    /**
     * 获取题目访问量排行
     */
    @GetMapping("/view-ranking")
    public Result<List<Map<String, Object>>> getViewRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("查询题目访问量排行：limit={}", limit);
        List<Map<String, Object>> ranking = problemCacheService.getViewRanking(limit);
        return Result.success(ranking);
    }
    
    /**
     * 获取题目统计信息（使用缓存）
     */
    @GetMapping("/stats/{id}")
    public Result<Map<String, Object>> getProblemStats(@PathVariable Long id) {
        log.info("查询题目统计信息：id={}", id);
        Map<String, Object> stats = problemCacheService.getProblemStats(id);
        return Result.success(stats);
    }
    
    /**
     * 获取题目列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getProblemList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("查询题目列表：pageNum={}, pageSize={}", pageNum, pageSize);
        
        List<Problem> problems = problemService.getAllProblems(pageNum, pageSize);
        Long total = problemService.getTotalCount();
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", problems);
        data.put("total", total);
        data.put("pageNum", pageNum);
        data.put("pageSize", pageSize);
        
        return Result.success(data);
    }
    
    /**
     * 根据难度查询题目
     */
    @GetMapping("/difficulty/{difficulty}")
    public Result<List<Problem>> getProblemsByDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("根据难度查询题目：difficulty={}", difficulty);
        List<Problem> problems = problemService.getProblemsByDifficulty(difficulty, pageNum, pageSize);
        return Result.success(problems);
    }
    
    /**
     * 搜索题目
     */
    @GetMapping("/search")
    public Result<List<Problem>> searchProblems(
            @RequestParam String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("搜索题目：title={}", title);
        List<Problem> problems = problemService.searchProblemsByTitle(title, pageNum, pageSize);
        return Result.success(problems);
    }
    
    /**
     * 创建题目
     */
    @PostMapping("/create")
    public Result<Long> createProblem(@Validated @RequestBody Problem problem) {
        log.info("创建题目：title={}", problem.getTitle());
        Long problemId = problemService.createProblem(problem);
        return Result.success("创建成功", problemId);
    }
    
    /**
     * 更新题目
     */
    @PutMapping("/update")
    public Result<Void> updateProblem(@Validated @RequestBody Problem problem) {
        log.info("更新题目：id={}", problem.getId());
        problemService.updateProblem(problem);
        // 清除缓存
        problemCacheService.clearProblemCache(problem.getId());
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProblem(@PathVariable Long id) {
        log.info("删除题目：id={}", id);
        problemService.deleteProblem(id);
        // 清除缓存
        problemCacheService.clearProblemCache(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 统计题目总数
     */
    @GetMapping("/count")
    public Result<Long> getTotalCount() {
        Long count = problemService.getTotalCount();
        return Result.success(count);
    }
    
    /**
     * 更新题目提交统计
     * @param id 题目ID
     * @param isAccepted 是否通过
     */
    @PostMapping("/updateStats/{id}")
    public Result<Void> updateSubmitStats(
            @PathVariable Long id,
            @RequestParam Boolean isAccepted) {
        log.info("更新题目统计：id={}, isAccepted={}", id, isAccepted);
        problemService.updateSubmitCount(id, isAccepted);
        return Result.success("更新成功", null);
    }
    
    /**
     * 测试接口：查看题目统计数据的JSON格式
     */
    @GetMapping("/testStats")
    public Result<Map<String, Object>> testStats() {
        List<Problem> problems = problemService.getAllProblems(1, 10);
        Map<String, Object> result = new HashMap<>();
        result.put("problems", problems);
        
        // 单独展示第一个题目的统计数据
        if (!problems.isEmpty()) {
            Problem first = problems.get(0);
            Map<String, Object> stats = new HashMap<>();
            stats.put("id", first.getId());
            stats.put("title", first.getTitle());
            stats.put("acceptCount", first.getAcceptCount());
            stats.put("submitCount", first.getSubmitCount());
            stats.put("acceptCount_type", first.getAcceptCount() != null ? first.getAcceptCount().getClass().getName() : "null");
            stats.put("submitCount_type", first.getSubmitCount() != null ? first.getSubmitCount().getClass().getName() : "null");
            result.put("firstProblemStats", stats);
        }
        
        return Result.success(result);
    }
    
    // ===================== 测试用例相关API =====================
    
    /**
     * 获取题目的所有测试用例（供judge-service内部调用）
     * @param id 题目ID
     * @return 测试用例列表
     */
    @GetMapping("/{id}/testcases")
    public Result<List<TestCase>> getTestCases(@PathVariable Long id) {
        log.info("获取题目测试用例：problemId={}", id);
        List<TestCase> testCases = testCaseMapper.selectByProblemId(id);
        return Result.success(testCases);
    }
    
    /**
     * 获取题目的样例测试用例（供普通用户查看）
     * @param id 题目ID
     * @return 样例测试用例列表
     */
    @GetMapping("/{id}/samples")
    public Result<List<TestCase>> getSampleTestCases(@PathVariable Long id) {
        log.info("获取题目样例：problemId={}", id);
        List<TestCase> samples = testCaseMapper.selectSamplesByProblemId(id);
        return Result.success(samples);
    }
    
    /**
     * 管理员：批量保存测试用例
     * @param id 题目ID
     * @param testCases 测试用例列表
     * @return 成功消息
     */
    @PostMapping("/{id}/testcases")
    public Result<Void> saveTestCases(
            @PathVariable Long id,
            @RequestBody List<TestCase> testCases) {
        log.info("保存测试用例：problemId={}, count={}", id, testCases.size());
        
        // 先删除旧的测试用例
        testCaseMapper.deleteByProblemId(id);
        
        // 设置problemId并插入新的测试用例
        if (testCases != null && !testCases.isEmpty()) {
            testCases.forEach(tc -> tc.setProblemId(id));
            testCaseMapper.batchInsert(testCases);
        }
        
        return Result.success("保存成功", null);
    }
    
    /**
     * 统计题目的测试用例数量
     * @param id 题目ID
     * @return 测试用例数量
     */
    @GetMapping("/{id}/testcases/count")
    public Result<Integer> getTestCaseCount(@PathVariable Long id) {
        int count = testCaseMapper.countByProblemId(id);
        return Result.success(count);
    }
}
