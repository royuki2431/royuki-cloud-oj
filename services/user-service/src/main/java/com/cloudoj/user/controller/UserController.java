package com.cloudoj.user.controller;

import com.cloudoj.model.common.Result;
import com.cloudoj.model.dto.user.ChangePasswordRequest;
import com.cloudoj.model.dto.user.LoginRequest;
import com.cloudoj.model.dto.user.RegisterRequest;
import com.cloudoj.model.dto.user.UpdateUserRequest;
import com.cloudoj.model.vo.user.LoginVO;
import com.cloudoj.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserService userService;
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Long> register(@Validated @RequestBody RegisterRequest request) {
        log.info("用户注册请求：username={}", request.getUsername());
        Long userId = userService.register(request);
        return Result.success("注册成功", userId);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginRequest request, 
                                   HttpServletRequest httpRequest) {
        log.info("用户登录请求：username={}, password长度={}", request.getUsername(), request.getPassword().length());
        log.debug("接收到的密码：{}", request.getPassword());
        LoginVO loginVO = userService.login(request);
        
        // 更新最后登录时间和IP
        String ip = getIpAddress(httpRequest);
        userService.updateLastLogin(loginVO.getUserId(), ip);
        
        return Result.success("登录成功", loginVO);
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/info/{userId}")
    public Result<com.cloudoj.model.entity.user.User> getUserInfo(@PathVariable Long userId) {
        log.info("查询用户信息：userId={}", userId);
        com.cloudoj.model.entity.user.User user = userService.getUserById(userId);
        // 隐藏密码
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Validated @RequestBody UpdateUserRequest request) {
        log.info("更新用户信息：userId={}", request.getId());
        userService.updateUserInfo(request);
        return Result.success("更新成功", null);
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        log.info("修改密码：userId={}", request.getUserId());
        userService.changePassword(request);
        return Result.success("密码修改成功", null);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
