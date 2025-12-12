package com.cloudoj.user.service.impl;

import com.cloudoj.model.constant.StatusCode;
import com.cloudoj.model.dto.user.ChangePasswordRequest;
import com.cloudoj.model.dto.user.LoginRequest;
import com.cloudoj.model.dto.user.RegisterRequest;
import com.cloudoj.model.dto.user.UpdateUserRequest;
import com.cloudoj.model.entity.user.User;
import com.cloudoj.model.enums.RoleEnum;
import com.cloudoj.model.vo.user.LoginVO;
import com.cloudoj.user.exception.BusinessException;
import com.cloudoj.user.mapper.UserMapper;
import com.cloudoj.user.service.UserService;
import com.cloudoj.user.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    UserMapper userMapper;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    PasswordEncoder passwordEncoder;
    
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
        log.debug("输入密码：{}, 数据库密码：{}", request.getPassword(), user.getPassword());
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.debug("密码匹配结果：{}", matches);
        if (!matches) {
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
    
    // ==================== 管理员功能实现 ====================
    
    @Override
    public List<User> getUserList(String keyword, String role, Integer status, int offset, int limit) {
        return userMapper.selectUserList(keyword, role, status, offset, limit);
    }
    
    @Override
    public int countUsers(String keyword, String role, Integer status) {
        return userMapper.countUsers(keyword, role, status);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(User user) {
        // 1. 检查用户名是否已存在
        User existUser = getUserByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException(StatusCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        
        // 2. 检查邮箱是否已存在
        User emailUser = userMapper.selectByEmail(user.getEmail());
        if (emailUser != null) {
            throw new BusinessException(StatusCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        }
        
        // 3. 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 4. 插入用户
        userMapper.insertSelective(user);
        
        log.info("管理员创建用户成功：username={}, userId={}", user.getUsername(), user.getId());
        
        return user.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        // 检查用户是否存在
        User existUser = getUserById(user.getId());
        if (existUser == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 更新用户（密码不更新）
        user.setPassword(null);
        int rows = userMapper.updateByPrimaryKeySelective(user);
        
        log.info("管理员更新用户成功：userId={}", user.getId());
        
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        
        int rows = userMapper.updateByPrimaryKeySelective(user);
        
        log.info("管理员更新用户状态成功：userId={}, status={}", userId, status);
        
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId) {
        // 检查用户是否存在
        User existUser = getUserById(userId);
        if (existUser == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 重置密码为123456
        String newPassword = passwordEncoder.encode("123456");
        
        User user = new User();
        user.setId(userId);
        user.setPassword(newPassword);
        
        int rows = userMapper.updateByPrimaryKeySelective(user);
        
        log.info("管理员重置用户密码成功：userId={}", userId);
        
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        // 检查用户是否存在
        User existUser = getUserById(userId);
        if (existUser == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        int rows = userMapper.deleteByPrimaryKey(userId);
        
        log.info("管理员删除用户成功：userId={}", userId);
        
        return rows > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UpdateUserRequest request) {
        // 检查用户是否存在
        User existUser = getUserById(request.getId());
        if (existUser == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 如果修改了邮箱，检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(existUser.getEmail())) {
            User emailUser = userMapper.selectByEmail(request.getEmail());
            if (emailUser != null && !emailUser.getId().equals(request.getId())) {
                throw new BusinessException(StatusCode.USER_ALREADY_EXISTS, "邮箱已被其他用户使用");
            }
        }
        
        // 更新用户信息
        User user = new User();
        user.setId(request.getId());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSchool(request.getSchool());
        user.setStudentId(request.getStudentId());
        user.setGrade(request.getGrade());
        user.setMajor(request.getMajor());
        user.setAvatar(request.getAvatar());
        
        userMapper.updateByPrimaryKeySelective(user);
        
        log.info("用户更新个人信息成功：userId={}", request.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        // 检查用户是否存在
        User existUser = getUserById(request.getUserId());
        if (existUser == null) {
            throw new BusinessException(StatusCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), existUser.getPassword())) {
            throw new BusinessException(StatusCode.PASSWORD_ERROR, "原密码错误");
        }
        
        // 更新密码
        User user = new User();
        user.setId(request.getUserId());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        
        userMapper.updateByPrimaryKeySelective(user);
        
        log.info("用户修改密码成功：userId={}", request.getUserId());
    }
}
