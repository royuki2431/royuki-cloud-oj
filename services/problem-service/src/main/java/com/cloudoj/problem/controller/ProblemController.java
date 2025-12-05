package com.cloudoj.problem.controller;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import com.cloudoj.problem.service.ProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/problem")
public class ProblemController {
    
    @Autowired
    ProblemService problemService;
    
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
     * 获取题目详情
     */
    @GetMapping("/{id}")
    public Result<Problem> getProblemById(@PathVariable Long id) {
        log.info("查询题目详情：id={}", id);
        Problem problem = problemService.getProblemById(id);
        return Result.success(problem);
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
        return Result.success("更新成功", null);
    }
    
    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProblem(@PathVariable Long id) {
        log.info("删除题目：id={}", id);
        problemService.deleteProblem(id);
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
}
