package com.cloudoj.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 用户服务启动类
 */
@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.cloudoj.user.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.cloudoj.user", "com.cloudoj.common"})
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("用户服务启动成功！");
        System.out.println("服务端口: 8081");
        System.out.println("========================================");
    }
}
