package com.cloudoj.model.dto.user;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 更新用户信息请求
 */
@Data
public class UpdateUserRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long id;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 学校
     */
    private String school;
    
    /**
     * 学号/工号
     */
    private String studentId;
    
    /**
     * 年级
     */
    private String grade;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 头像URL
     */
    private String avatar;
}
