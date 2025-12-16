package com.cloudoj.learning.service.impl;

import com.cloudoj.learning.entity.WrongProblem;
import com.cloudoj.learning.mapper.WrongProblemMapper;
import com.cloudoj.learning.service.WrongProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 错题本服务实现类
 */
@Slf4j
@Service
public class WrongProblemServiceImpl implements WrongProblemService {
    
    @Autowired
    private WrongProblemMapper wrongProblemMapper;
    
    @Override
    @Transactional
    public void addWrongProblem(Long userId, Long problemId, Long submissionId, String errorType) {
        // 检查是否已存在该题的错题记录
        WrongProblem existing = wrongProblemMapper.selectByUserAndProblem(userId, problemId);
        
        if (existing != null) {
            // 更新错误次数
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setSubmissionId(submissionId);
            existing.setErrorType(errorType);
            existing.setIsResolved(0); // 重新标记为未解决
            existing.setResolvedTime(null);
            wrongProblemMapper.updateByPrimaryKeySelective(existing);
            log.info("更新错题记录：userId={}, problemId={}, wrongCount={}", userId, problemId, existing.getWrongCount());
        } else {
            // 创建新记录
            WrongProblem wrongProblem = new WrongProblem();
            wrongProblem.setUserId(userId);
            wrongProblem.setProblemId(problemId);
            wrongProblem.setSubmissionId(submissionId);
            wrongProblem.setErrorType(errorType);
            wrongProblem.setWrongCount(1);
            wrongProblem.setIsResolved(0);
            wrongProblemMapper.insertSelective(wrongProblem);
            log.info("添加错题记录：userId={}, problemId={}", userId, problemId);
        }
    }
    
    @Override
    public List<WrongProblem> getUserWrongProblems(Long userId) {
        return wrongProblemMapper.selectByUserId(userId);
    }
    
    @Override
    public List<WrongProblem> getUnresolvedWrongProblems(Long userId) {
        return wrongProblemMapper.selectUnresolvedByUserId(userId);
    }
    
    @Override
    @Transactional
    public void resolveWrongProblem(Long userId, Long problemId) {
        WrongProblem wrongProblem = wrongProblemMapper.selectByUserAndProblem(userId, problemId);
        if (wrongProblem != null) {
            wrongProblem.setIsResolved(1);
            wrongProblem.setResolvedTime(LocalDateTime.now());
            wrongProblemMapper.updateByPrimaryKeySelective(wrongProblem);
            log.info("标记错题已解决：userId={}, problemId={}", userId, problemId);
        }
    }
    
    @Override
    @Transactional
    public void deleteWrongProblem(Long id, Long userId) {
        WrongProblem wrongProblem = wrongProblemMapper.selectByPrimaryKey(id);
        if (wrongProblem != null && wrongProblem.getUserId().equals(userId)) {
            wrongProblemMapper.deleteByPrimaryKey(id);
            log.info("删除错题记录：id={}, userId={}", id, userId);
        }
    }
    
    @Override
    public Map<String, Object> getWrongProblemStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<WrongProblem> allWrong = wrongProblemMapper.selectByUserId(userId);
        List<WrongProblem> unresolved = wrongProblemMapper.selectUnresolvedByUserId(userId);
        
        stats.put("totalCount", allWrong.size());
        stats.put("unresolvedCount", unresolved.size());
        stats.put("resolvedCount", allWrong.size() - unresolved.size());
        
        // 按错误类型统计
        Map<String, Integer> byErrorType = new HashMap<>();
        for (WrongProblem wp : allWrong) {
            String type = wp.getErrorType() != null ? wp.getErrorType() : "OTHER";
            byErrorType.put(type, byErrorType.getOrDefault(type, 0) + 1);
        }
        stats.put("byErrorType", byErrorType);
        
        return stats;
    }
    
    @Override
    public List<WrongProblem> getWrongProblemsByErrorType(Long userId, String errorType) {
        return wrongProblemMapper.selectByUserAndErrorType(userId, errorType);
    }
}
