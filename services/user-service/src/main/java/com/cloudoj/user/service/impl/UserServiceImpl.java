package com.cloudoj.user.service.impl;

import com.cloudoj.model.constant.StatusCode;
import com.cloudoj.model.dto.user.LoginRequest;
import com.cloudoj.model.dto.user.RegisterRequest;
import com.cloudoj.model.entity.user.User;
import com.cloudoj.model.enums.RoleEnum;
import com.cloudoj.model.vo.user.LoginVO;
import com.cloudoj.user.exception.BusinessException;
import com.cloudoj.user.mapper.UserMapper;
import com.cloudoj.user.service.UserService;
import com.cloudoj.user.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    JwtUtil jwtUtil;
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        // 1. 检查用户名是否已存在
        User existUser = getUserByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(StatusCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        
        // 2. 检查邮箱是否已存在
        User emailUser = userMapper.selectByEmail(request.getEmail());
        if (emailUser != null) {
            throw new BusinessException(StatusCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        }
        
        // 3. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt加密
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setSchool(request.getSchool());
        user.setStudentId(request.getStudentId());
        user.setGrade(request.getGrade());
        user.setMajor(request.getMajor());
        user.setRole(RoleEnum.STUDENT.getCode());  // 默认角色为学生
        user.setStatus(1);  // 正常状态
        
        userMapper.insertSelective(user);
        log.info("用户注册成功：username={}, userId={}", request.getUsername(), user.getId());
        
        return user.getId();
    }
    
    @Override
    public LoginVO login(LoginRequest request) {
        // 1. 查询用户
        User user = getUserByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(StatusCode.USER_DISABLED, "账号已被禁用");
        }
        
        // 4. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 5. 构建响应
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setEmail(user.getEmail());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());
        loginVO.setToken(token);
        loginVO.setExpireTime(jwtUtil.getExpireTime());
        
        log.info("用户登录成功：username={}, userId={}", user.getUsername(), user.getId());
        
        return loginVO;
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectByPrimaryKey(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, String ip) {
        String loginTime = java.time.LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        userMapper.updateLastLogin(userId, loginTime, ip);
    }
}
