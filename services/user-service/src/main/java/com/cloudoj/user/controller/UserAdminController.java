package com.cloudoj.user.controller;

import com.cloudoj.common.annotation.RequireRole;
import com.cloudoj.model.common.Result;
import com.cloudoj.model.entity.user.User;
import com.cloudoj.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器（管理员）
 * 需要 ADMIN 或 SUPER_ADMIN 角色
 */
@Slf4j
@RestController
@RequestMapping("/user/admin")
@RequireRole({"ADMIN", "SUPER_ADMIN"})
public class UserAdminController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("获取用户列表：keyword={}, role={}, status={}, page={}, size={}", 
                keyword, role, status, page, size);
        
        // 计算offset
        int offset = (page - 1) * size;
        
        // 查询用户列表
        List<User> users = userService.getUserList(keyword, role, status, offset, size);
        
        // 查询总数
        int total = userService.countUsers(keyword, role, status);
        
        // 隐藏密码
        users.forEach(user -> user.setPassword(null));
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        
        return Result.success(result);
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/create")
    public Result<Long> createUser(@RequestBody User user) {
        log.info("创建用户：username={}", user.getUsername());
        
        Long userId = userService.createUser(user);
        
        return Result.success("创建成功", userId);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/update/{userId}")
    public Result<Boolean> updateUser(@PathVariable Long userId, @RequestBody User user) {
        log.info("更新用户：userId={}", userId);
        
        user.setId(userId);
        boolean success = userService.updateUser(user);
        
        return Result.success("更新成功", success);
    }
    
    /**
     * 更新用户状态
     */
    @PutMapping("/status/{userId}")
    public Result<Boolean> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> request) {
        
        Integer status = request.get("status");
        log.info("更新用户状态：userId={}, status={}", userId, status);
        
        boolean success = userService.updateUserStatus(userId, status);
        
        return Result.success("状态更新成功", success);
    }
    
    /**
     * 重置用户密码
     */
    @PutMapping("/reset-password/{userId}")
    public Result<Boolean> resetPassword(@PathVariable Long userId) {
        log.info("重置用户密码：userId={}", userId);
        
        boolean success = userService.resetPassword(userId);
        
        return Result.success("密码重置成功", success);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{userId}")
    public Result<Boolean> deleteUser(@PathVariable Long userId) {
        log.info("删除用户：userId={}", userId);
        
        boolean success = userService.deleteUser(userId);
        
        return Result.success("删除成功", success);
    }
}
