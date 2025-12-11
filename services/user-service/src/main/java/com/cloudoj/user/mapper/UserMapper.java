package com.cloudoj.user.mapper;

import com.cloudoj.model.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */

public interface UserMapper {
    
    /**
     * 根据主键删除（逻辑删除）
     */
    int deleteByPrimaryKey(Long id);
    
    /**
     * 插入用户（所有字段）
     */
    int insert(User record);
    
    /**
     * 插入用户（选择性插入非空字段）
     */
    int insertSelective(User record);
    
    /**
     * 根据主键查询
     */
    User selectByPrimaryKey(Long id);
    
    /**
     * 根据用户名查询
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询
     */
    User selectByEmail(@Param("email") String email);
    
    /**
     * 根据主键更新（选择性更新非空字段）
     */
    int updateByPrimaryKeySelective(User record);
    
    /**
     * 根据主键更新（所有字段）
     */
    int updateByPrimaryKey(User record);
    
    /**
     * 更新最后登录信息
     */
    int updateLastLogin(@Param("userId") Long userId, 
                        @Param("loginTime") String loginTime, 
                        @Param("loginIp") String loginIp);
    
    // ==================== 管理员功能 ====================
    
    /**
     * 查询用户列表
     */
    List<User> selectUserList(@Param("keyword") String keyword,
                              @Param("role") String role,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);
    
    /**
     * 统计用户数量
     */
    int countUsers(@Param("keyword") String keyword,
                   @Param("role") String role,
                   @Param("status") Integer status);
}
