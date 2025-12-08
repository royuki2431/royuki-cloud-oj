package com.cloudoj.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    
    @Test
    public void testPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "123456";
        String encodedPassword = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKqRPeOi";
        
        System.out.println("原始密码: " + rawPassword);
        System.out.println("数据库密码: " + encodedPassword);
        
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("密码匹配结果: " + matches);
        
        // 生成新的加密密码
        String newEncoded = encoder.encode(rawPassword);
        System.out.println("新生成的加密密码: " + newEncoded);
        
        boolean newMatches = encoder.matches(rawPassword, newEncoded);
        System.out.println("新密码匹配结果: " + newMatches);
    }
}
