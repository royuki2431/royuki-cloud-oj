package com.cloudoj.model.enums;

import lombok.Getter;

/**
 * 评测状态枚举
 */
@Getter
public enum JudgeStatusEnum {
    
    PENDING("PENDING", "等待评测"),
    JUDGING("JUDGING", "评测中"),
    ACCEPTED("ACCEPTED", "通过"),
    WRONG_ANSWER("WRONG_ANSWER", "答案错误"),
    TIME_LIMIT_EXCEEDED("TIME_LIMIT_EXCEEDED", "时间超限"),
    MEMORY_LIMIT_EXCEEDED("MEMORY_LIMIT_EXCEEDED", "内存超限"),
    RUNTIME_ERROR("RUNTIME_ERROR", "运行时错误"),
    COMPILE_ERROR("COMPILE_ERROR", "编译错误"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统错误");
    
    private final String code;
    private final String desc;
    
    JudgeStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static JudgeStatusEnum getByCode(String code) {
        for (JudgeStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
