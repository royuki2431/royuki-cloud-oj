package com.cloudoj.learning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 学习服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cloudoj.learning", "com.cloudoj.common"})
@EnableDiscoveryClient  // 启用服务发现
@EnableFeignClients     // 启用Feign客户端
@MapperScan("com.cloudoj.learning.mapper")  // 扫描Mapper接口
public class LearningServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LearningServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("学习服务启动成功！");
        System.out.println("服务端口: 8084");
        System.out.println("========================================");
    }
}
