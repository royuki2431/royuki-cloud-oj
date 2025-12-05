package com.cloudoj.user.service;

import com.cloudoj.model.dto.user.LoginRequest;
import com.cloudoj.model.dto.user.RegisterRequest;
import com.cloudoj.model.entity.user.User;
import com.cloudoj.model.vo.user.LoginVO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     * @param request 注册请求
     * @return 用户ID
     */
    Long register(RegisterRequest request);
    
    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录信息（包含Token）
     */
    LoginVO login(LoginRequest request);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);
    
    /**
     * 更新最后登录时间
     * @param userId 用户ID
     * @param ip IP地址
     */
    void updateLastLogin(Long userId, String ip);
}
