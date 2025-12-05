package com.cloudoj.model.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum RoleEnum {
    
    STUDENT("STUDENT", "学生"),
    TEACHER("TEACHER", "教师"),
    ADMIN("ADMIN", "管理员");
    
    private final String code;
    private final String desc;
    
    RoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public static RoleEnum getByCode(String code) {
        for (RoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
