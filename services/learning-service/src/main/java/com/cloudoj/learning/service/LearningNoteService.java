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
     * 获取所有公开笔记（笔记广场）
     */
    List<LearningNote> getAllPublicNotes(int page, int size);
    
    /**
     * 搜索笔记
     */
    List<LearningNote> searchNotes(Long userId, String keyword);
    
    /**
     * 增加笔记浏览次数
     */
    void incrementViewCount(Long noteId);
    
    /**
     * 管理员获取所有笔记（分页）
     */
    List<LearningNote> getAllNotes(int page, int size, String keyword, Integer isPublic);
    
    /**
     * 管理员统计笔记总数
     */
    long countAllNotes(String keyword, Integer isPublic);
    
    /**
     * 管理员删除笔记（无需验证用户）
     */
    void adminDeleteNote(Long id);
    
    /**
     * 管理员更新笔记公开状态
     */
    void updateNotePublicStatus(Long id, Integer isPublic);
}
