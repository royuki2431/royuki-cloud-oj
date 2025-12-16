package com.cloudoj.learning.service.impl;

import com.cloudoj.learning.entity.LearningNote;
import com.cloudoj.learning.feign.ProblemServiceClient;
import com.cloudoj.learning.feign.UserServiceClient;
import com.cloudoj.learning.mapper.LearningNoteMapper;
import com.cloudoj.learning.service.LearningNoteService;
import com.cloudoj.model.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 学习笔记服务实现类
 */
@Slf4j
@Service
public class LearningNoteServiceImpl implements LearningNoteService {
    
    @Autowired
    private LearningNoteMapper learningNoteMapper;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProblemServiceClient problemServiceClient;
    
    @Override
    @Transactional
    public Long createNote(LearningNote note) {
        learningNoteMapper.insertSelective(note);
        log.info("创建笔记：id={}, userId={}, problemId={}", note.getId(), note.getUserId(), note.getProblemId());
        return note.getId();
    }
    
    @Override
    @Transactional
    public void updateNote(LearningNote note) {
        // 验证笔记是否存在
        LearningNote existing = learningNoteMapper.selectByPrimaryKey(note.getId());
        if (existing == null) {
            throw new RuntimeException("笔记不存在");
        }
        
        // 如果请求中没有userId，使用原笔记的userId（允许用户修改自己的笔记）
        if (note.getUserId() == null) {
            note.setUserId(existing.getUserId());
        }
        
        // 验证所有权：只能修改自己的笔记
        if (!existing.getUserId().equals(note.getUserId())) {
            throw new RuntimeException("无权修改此笔记");
        }
        
        learningNoteMapper.updateByPrimaryKeySelective(note);
        log.info("更新笔记：id={}, userId={}", note.getId(), existing.getUserId());
    }
    
    @Override
    @Transactional
    public void deleteNote(Long id, Long userId) {
        LearningNote note = learningNoteMapper.selectByPrimaryKey(id);
        if (note != null && note.getUserId().equals(userId)) {
            learningNoteMapper.deleteByPrimaryKey(id);
            log.info("删除笔记：id={}, userId={}", id, userId);
        } else {
            throw new RuntimeException("无权删除此笔记");
        }
    }
    
    @Override
    public LearningNote getNoteById(Long id) {
        LearningNote note = learningNoteMapper.selectByPrimaryKey(id);
        if (note != null) {
            // 填充作者和题目信息
            fillNoteInfo(note);
        }
        return note;
    }
    
    /**
     * 填充单个笔记的作者和题目信息
     */
    private void fillNoteInfo(LearningNote note) {
        // 填充作者信息
        try {
            Result<Map<String, Object>> result = userServiceClient.getUserInfo(note.getUserId());
            if (result != null && result.getData() != null) {
                Map<String, Object> userInfo = result.getData();
                String realName = (String) userInfo.get("realName");
                String username = (String) userInfo.get("username");
                note.setAuthorName(realName != null ? realName : username);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败：userId={}", note.getUserId());
            note.setAuthorName("匿名用户");
        }
        
        // 填充题目信息
        if (note.getProblemId() != null && note.getProblemId() > 0) {
            try {
                Result<Map<String, Object>> result = problemServiceClient.getProblemById(note.getProblemId());
                if (result != null && result.getData() != null) {
                    Map<String, Object> problemInfo = result.getData();
                    String title = (String) problemInfo.get("title");
                    note.setProblemTitle(title != null ? title : "题目 #" + note.getProblemId());
                }
            } catch (Exception e) {
                log.warn("获取题目信息失败：problemId={}", note.getProblemId());
                note.setProblemTitle("题目 #" + note.getProblemId());
            }
        }
    }
    
    @Override
    public List<LearningNote> getUserNotes(Long userId) {
        return learningNoteMapper.selectByUserId(userId);
    }
    
    @Override
    public List<LearningNote> getProblemNotes(Long userId, Long problemId) {
        return learningNoteMapper.selectByUserAndProblem(userId, problemId);
    }
    
    @Override
    public List<LearningNote> getPublicNotes(Long problemId) {
        List<LearningNote> notes = learningNoteMapper.selectPublicByProblemId(problemId);
        // 填充作者信息
        fillAuthorInfo(notes);
        return notes;
    }
    
    @Override
    public List<LearningNote> getAllPublicNotes(int page, int size) {
        int offset = (page - 1) * size;
        List<LearningNote> notes = learningNoteMapper.selectAllPublicNotes(offset, size);
        // 填充作者信息
        fillAuthorInfo(notes);
        return notes;
    }
    
    @Override
    public List<LearningNote> searchNotes(Long userId, String keyword) {
        return learningNoteMapper.searchByKeyword(userId, "%" + keyword + "%");
    }
    
    @Override
    @Transactional
    public void incrementViewCount(Long noteId) {
        learningNoteMapper.incrementViewCount(noteId);
    }
    
    /**
     * 填充笔记列表的作者和题目信息
     */
    private void fillAuthorInfo(List<LearningNote> notes) {
        for (LearningNote note : notes) {
            fillNoteInfo(note);
        }
    }
    
    @Override
    public List<LearningNote> getAllNotes(int page, int size, String keyword, Integer isPublic) {
        int offset = (page - 1) * size;
        List<LearningNote> notes = learningNoteMapper.selectAllNotes(offset, size, keyword, isPublic);
        fillAuthorInfo(notes);
        return notes;
    }
    
    @Override
    public long countAllNotes(String keyword, Integer isPublic) {
        return learningNoteMapper.countAllNotes(keyword, isPublic);
    }
    
    @Override
    @Transactional
    public void adminDeleteNote(Long id) {
        learningNoteMapper.deleteByPrimaryKey(id);
    }
    
    @Override
    @Transactional
    public void updateNotePublicStatus(Long id, Integer isPublic) {
        learningNoteMapper.updatePublicStatus(id, isPublic);
    }
}
