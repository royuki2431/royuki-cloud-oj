package com.cloudoj.learning.service.impl;

import com.cloudoj.learning.entity.LearningNote;
import com.cloudoj.learning.mapper.LearningNoteMapper;
import com.cloudoj.learning.service.LearningNoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学习笔记服务实现类
 */
@Slf4j
@Service
public class LearningNoteServiceImpl implements LearningNoteService {
    
    @Autowired
    private LearningNoteMapper learningNoteMapper;
    
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
        return learningNoteMapper.selectByPrimaryKey(id);
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
        return learningNoteMapper.selectPublicByProblemId(problemId);
    }
    
    @Override
    public List<LearningNote> searchNotes(Long userId, String keyword) {
        return learningNoteMapper.searchByKeyword(userId, "%" + keyword + "%");
    }
}
