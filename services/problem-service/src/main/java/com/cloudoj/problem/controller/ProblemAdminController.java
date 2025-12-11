package com.cloudoj.problem.controller;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.problem.Problem;
import com.cloudoj.problem.service.ProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目管理控制器（管理员）
 */
@Slf4j
@RestController
@RequestMapping("/problem/admin")
public class ProblemAdminController {
    
    @Autowired
    private ProblemService problemService;
    
    /**
     * 获取题目列表（管理员）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getProblemList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("获取题目列表：keyword={}, difficulty={}, category={}, status={}, page={}, size={}", 
                keyword, difficulty, category, status, page, size);
        
        // 计算offset
        int offset = (page - 1) * size;
        
        // 查询题目列表
        List<Problem> problems = problemService.getProblemListAdmin(keyword, difficulty, category, status, offset, size);
        
        // 查询总数
        int total = problemService.countProblems(keyword, difficulty, category, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", problems);
        result.put("total", total);
        
        return Result.success(result);
    }
    
    /**
     * 创建题目
     */
    @PostMapping("/create")
    public Result<Long> createProblem(@RequestBody Problem problem) {
        log.info("创建题目：title={}", problem.getTitle());
        
        Long problemId = problemService.createProblem(problem);
        
        return Result.success("创建成功", problemId);
    }
    
    /**
     * 更新题目
     */
    @PutMapping("/update/{problemId}")
    public Result<Boolean> updateProblem(@PathVariable Long problemId, @RequestBody Problem problem) {
        log.info("更新题目：problemId={}", problemId);
        
        problem.setId(problemId);
        problemService.updateProblem(problem);
        
        return Result.success("更新成功", true);
    }
    
    /**
     * 更新题目状态
     */
    @PutMapping("/status/{problemId}")
    public Result<Boolean> updateProblemStatus(
            @PathVariable Long problemId,
            @RequestBody Map<String, Integer> request) {
        
        Integer status = request.get("status");
        log.info("更新题目状态：problemId={}, status={}", problemId, status);
        
        problemService.updateProblemStatus(problemId, status);
        
        return Result.success("状态更新成功", true);
    }
    
    /**
     * 删除题目
     */
    @DeleteMapping("/delete/{problemId}")
    public Result<Boolean> deleteProblem(@PathVariable Long problemId) {
        log.info("删除题目：problemId={}", problemId);
        
        problemService.deleteProblem(problemId);
        
        return Result.success("删除成功", true);
    }
}
