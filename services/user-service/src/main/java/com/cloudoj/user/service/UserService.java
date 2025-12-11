package com.cloudoj.user.service;

import com.cloudoj.model.dto.user.LoginRequest;
import com.cloudoj.model.dto.user.RegisterRequest;
import com.cloudoj.model.entity.user.User;
import com.cloudoj.model.vo.user.LoginVO;

import java.util.List;

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
    
    // ==================== 管理员功能 ====================
    
    /**
     * 获取用户列表
     * @param keyword 搜索关键词
     * @param role 角色
     * @param status 状态
     * @param offset 偏移量
     * @param limit 数量
     * @return 用户列表
     */
    List<User> getUserList(String keyword, String role, Integer status, int offset, int limit);
    
    /**
     * 统计用户数量
     * @param keyword 搜索关键词
     * @param role 角色
     * @param status 状态
     * @return 用户数量
     */
    int countUsers(String keyword, String role, Integer status);
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 用户ID
     */
    Long createUser(User user);
    
    /**
     * 更新用户
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUser(User user);
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateUserStatus(Long userId, Integer status);
    
    /**
     * 重置密码
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean resetPassword(Long userId);
    
    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);
}
