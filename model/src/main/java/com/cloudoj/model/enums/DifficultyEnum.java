package com.cloudoj.model.enums;

import lombok.Getter;

/**
 * 题目难度枚举
 */
@Getter
public enum DifficultyEnum {
    
    EASY("EASY", "简单", 1),
    MEDIUM("MEDIUM", "中等", 2),
    HARD("HARD", "困难", 3);
    
    private final String code;
    private final String desc;
    private final Integer level;
    
    DifficultyEnum(String code, String desc, Integer level) {
        this.code = code;
        this.desc = desc;
        this.level = level;
    }
    
    public static DifficultyEnum getByCode(String code) {
        for (DifficultyEnum difficulty : values()) {
            if (difficulty.getCode().equals(code)) {
                return difficulty;
            }
        }
        return null;
    }
}
