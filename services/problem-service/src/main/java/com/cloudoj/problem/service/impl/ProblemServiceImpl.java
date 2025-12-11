package com.cloudoj.problem.service.impl;

import com.cloudoj.model.entity.problem.Problem;
import com.cloudoj.problem.mapper.ProblemMapper;
import com.cloudoj.problem.service.ProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 题目服务实现类
 */
@Slf4j
@Service
public class ProblemServiceImpl implements ProblemService {
    
    @Autowired
    ProblemMapper problemMapper;
    
    @Override
    public Problem getProblemById(Long id) {
        if (id == null) {
            throw new RuntimeException("题目ID不能为空");
        }
        Problem problem = problemMapper.selectByPrimaryKey(id);
        if (problem == null) {
            throw new RuntimeException("题目不存在");
        }
        return problem;
    }
    
    @Override
    public List<Problem> getAllProblems(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return problemMapper.selectAll(offset, pageSize);
    }
    
    @Override
    public List<Problem> getProblemsByDifficulty(String difficulty, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return problemMapper.selectByDifficulty(difficulty, offset, pageSize);
    }
    
    @Override
    public List<Problem> searchProblemsByTitle(String title, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return problemMapper.selectByTitle(title, offset, pageSize);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProblem(Problem problem) {
        // 参数校验
        if (problem.getTitle() == null || problem.getTitle().trim().isEmpty()) {
            throw new RuntimeException("题目标题不能为空");
        }
        if (problem.getDescription() == null || problem.getDescription().trim().isEmpty()) {
            throw new RuntimeException("题目描述不能为空");
        }
        
        // 设置默认值
        if (problem.getStatus() == null) {
            problem.setStatus(1);
        }
        if (problem.getIsPublic() == null) {
            problem.setIsPublic(1);
        }
        if (problem.getTimeLimit() == null) {
            problem.setTimeLimit(1000);
        }
        if (problem.getMemoryLimit() == null) {
            problem.setMemoryLimit(256);
        }
        if (problem.getDifficulty() == null) {
            problem.setDifficulty("MEDIUM");
        }
        
        problemMapper.insertSelective(problem);
        log.info("创建题目成功，ID: {}", problem.getId());
        return problem.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProblem(Problem problem) {
        if (problem.getId() == null) {
            throw new RuntimeException("题目ID不能为空");
        }
        
        Problem existProblem = problemMapper.selectByPrimaryKey(problem.getId());
        if (existProblem == null) {
            throw new RuntimeException("题目不存在");
        }
        
        int rows = problemMapper.updateByPrimaryKeySelective(problem);
        if (rows == 0) {
            throw new RuntimeException("更新题目失败");
        }
        log.info("更新题目成功，ID: {}", problem.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProblem(Long id) {
        if (id == null) {
            throw new RuntimeException("题目ID不能为空");
        }
        
        Problem existProblem = problemMapper.selectByPrimaryKey(id);
        if (existProblem == null) {
            throw new RuntimeException("题目不存在");
        }
        
        int rows = problemMapper.deleteByPrimaryKey(id);
        if (rows == 0) {
            throw new RuntimeException("删除题目失败");
        }
        log.info("删除题目成功，ID: {}", id);
    }
    
    @Override
    public Long getTotalCount() {
        return problemMapper.countAll();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubmitCount(Long id, Boolean isAccepted) {
        if (id == null) {
            throw new RuntimeException("题目ID不能为空");
        }
        problemMapper.updateSubmitCount(id, isAccepted);
        log.info("更新题目提交统计成功，ID: {}, isAccepted: {}", id, isAccepted);
    }
    
    // ==================== 管理员功能实现 ====================
    
    @Override
    public List<Problem> getProblemListAdmin(String keyword, String difficulty, String category, Integer status, int offset, int limit) {
        return problemMapper.selectProblemListAdmin(keyword, difficulty, category, status, offset, limit);
    }
    
    @Override
    public int countProblems(String keyword, String difficulty, String category, Integer status) {
        return problemMapper.countProblems(keyword, difficulty, category, status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProblemStatus(Long problemId, Integer status) {
        if (problemId == null) {
            throw new RuntimeException("题目ID不能为空");
        }
        
        Problem existProblem = problemMapper.selectByPrimaryKey(problemId);
        if (existProblem == null) {
            throw new RuntimeException("题目不存在");
        }
        
        Problem problem = new Problem();
        problem.setId(problemId);
        problem.setStatus(status);
        
        int rows = problemMapper.updateByPrimaryKeySelective(problem);
        if (rows == 0) {
            throw new RuntimeException("更新题目状态失败");
        }
        
        log.info("管理员更新题目状态成功：problemId={}, status={}", problemId, status);
    }
}
