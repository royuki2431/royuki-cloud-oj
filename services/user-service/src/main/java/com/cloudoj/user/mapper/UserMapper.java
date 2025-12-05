package com.cloudoj.user.mapper;

import com.cloudoj.model.entity.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
