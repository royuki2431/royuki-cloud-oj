package com.cloudoj.judge.service;

import com.cloudoj.model.dto.judge.SubmitCodeRequest;
import com.cloudoj.model.entity.judge.Submission;
import com.cloudoj.model.vo.judge.JudgeResultVO;
import com.cloudoj.model.vo.judge.SubmissionVO;

import java.util.List;

/**
 * 评测服务接口
 */
public interface JudgeService {
    
    /**
     * 提交代码
     */
    Long submitCode(SubmitCodeRequest request, String ipAddress);
    
    /**
     * 执行评测
     */
    JudgeResultVO executeJudge(Long submissionId);
    
    /**
     * 查询提交记录详情
     */
    SubmissionVO getSubmissionById(Long id);
    
    /**
     * 查询用户的提交记录列表
     */
    List<SubmissionVO> getUserSubmissions(Long userId, int page, int size);
    
    /**
     * 查询题目的提交记录列表
     */
    List<SubmissionVO> getProblemSubmissions(Long problemId, int page, int size);
    
    /**
     * 查询评测结果
     */
    JudgeResultVO getJudgeResult(Long submissionId);
}
