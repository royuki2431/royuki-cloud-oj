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
 * 题目管理控制器（管理员/教师）
 * 教师只能管理自己创建的题目
 */
@Slf4j
@RestController
@RequestMapping("/problem/admin")
public class ProblemAdminController {
    
    @Autowired
    private ProblemService problemService;
    
    /**
     * 获取题目列表（管理员/教师）
     * @param authorId 教师ID，传入时只查询该教师创建的题目
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getProblemList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("获取题目列表：keyword={}, difficulty={}, category={}, status={}, authorId={}, page={}, size={}", 
                keyword, difficulty, category, status, authorId, page, size);
        
        // 计算offset
        int offset = (page - 1) * size;
        
        // 查询题目列表（传入authorId时只查询该作者的题目）
        List<Problem> problems = problemService.getProblemListAdmin(keyword, difficulty, category, status, authorId, offset, size);
        
        // 查询总数
        int total = problemService.countProblems(keyword, difficulty, category, status, authorId);
        
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
     * @param authorId 当前用户ID，用于权限校验
     */
    @PutMapping("/update/{problemId}")
    public Result<Boolean> updateProblem(
            @PathVariable Long problemId, 
            @RequestParam(required = false) Long authorId,
            @RequestBody Problem problem) {
        log.info("更新题目：problemId={}, authorId={}", problemId, authorId);
        
        // 权限校验：检查题目是否属于该作者
        if (authorId != null) {
            Problem existProblem = problemService.getProblemById(problemId);
            if (existProblem.getAuthorId() != null && !existProblem.getAuthorId().equals(authorId)) {
                return Result.error("无权限修改此题目，只能修改自己创建的题目");
            }
        }
        
        problem.setId(problemId);
        problemService.updateProblem(problem);
        
        return Result.success("更新成功", true);
    }
    
    /**
     * 更新题目状态
     * @param authorId 当前用户ID，用于权限校验
     */
    @PutMapping("/status/{problemId}")
    public Result<Boolean> updateProblemStatus(
            @PathVariable Long problemId,
            @RequestParam(required = false) Long authorId,
            @RequestBody Map<String, Integer> request) {
        
        Integer status = request.get("status");
        log.info("更新题目状态：problemId={}, status={}, authorId={}", problemId, status, authorId);
        
        // 权限校验：检查题目是否属于该作者
        if (authorId != null) {
            Problem existProblem = problemService.getProblemById(problemId);
            if (existProblem.getAuthorId() != null && !existProblem.getAuthorId().equals(authorId)) {
                return Result.error("无权限修改此题目状态，只能修改自己创建的题目");
            }
        }
        
        problemService.updateProblemStatus(problemId, status);
        
        return Result.success("状态更新成功", true);
    }
    
    /**
     * 删除题目
     * @param authorId 当前用户ID，用于权限校验
     */
    @DeleteMapping("/delete/{problemId}")
    public Result<Boolean> deleteProblem(
            @PathVariable Long problemId,
            @RequestParam(required = false) Long authorId) {
        log.info("删除题目：problemId={}, authorId={}", problemId, authorId);
        
        // 权限校验：检查题目是否属于该作者
        if (authorId != null) {
            Problem existProblem = problemService.getProblemById(problemId);
            if (existProblem.getAuthorId() != null && !existProblem.getAuthorId().equals(authorId)) {
                return Result.error("无权限删除此题目，只能删除自己创建的题目");
            }
        }
        
        problemService.deleteProblem(problemId);
        
        return Result.success("删除成功", true);
    }
}
