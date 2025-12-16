package com.cloudoj.learning.service;

import com.cloudoj.learning.entity.LearningNote;

import java.util.List;

/**
 * 学习笔记服务接口
 */
public interface LearningNoteService {
    
    /**
     * 创建笔记
     */
    Long createNote(LearningNote note);
    
    /**
     * 更新笔记
     */
    void updateNote(LearningNote note);
    
    /**
     * 删除笔记
     */
    void deleteNote(Long id, Long userId);
    
    /**
     * 获取笔记详情
     */
    LearningNote getNoteById(Long id);
    
    /**
     * 获取用户的笔记列表
     */
    List<LearningNote> getUserNotes(Long userId);
    
    /**
     * 获取题目的笔记列表（用户自己的）
     */
    List<LearningNote> getProblemNotes(Long userId, Long problemId);
    
    /**
     * 获取公开的笔记列表
     */
    List<LearningNote> getPublicNotes(Long problemId);
    
    /**
     * 搜索笔记
     */
    List<LearningNote> searchNotes(Long userId, String keyword);
}
