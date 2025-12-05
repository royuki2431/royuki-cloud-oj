package com.cloudoj.judge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 评测服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用服务发现
@EnableFeignClients     // 启用Feign客户端
@MapperScan("com.cloudoj.judge.mapper")  // 扫描Mapper接口
public class JudgeServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(JudgeServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("评测服务启动成功！");
        System.out.println("服务端口: 8083");
        System.out.println("========================================");
    }
}
