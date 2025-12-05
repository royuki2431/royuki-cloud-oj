package com.cloudoj.problem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 题库服务启动类
 */
@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.cloudoj.problem.mapper")
@EnableDiscoveryClient
@SpringBootApplication
public class ProblemServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ProblemServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("题库服务启动成功！");
        System.out.println("服务端口: 8082");
        System.out.println("========================================");
    }
}
