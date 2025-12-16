package com.cloudoj.course;

import com.cloudoj.course.service.ClassService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * 课程服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.cloudoj.course", "com.cloudoj.common"})
@EnableDiscoveryClient  // 启用服务发现
@EnableFeignClients     // 启用Feign客户端
@MapperScan("com.cloudoj.course.mapper")  // 扫描Mapper接口
public class CourseServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("课程服务启动成功！");
        System.out.println("服务端口: 8085");
        System.out.println("========================================");
    }
    
    /**
     * 服务启动时同步学生数量
     */
    @Bean
    public CommandLineRunner syncStudentCounts(ClassService classService) {
        return args -> {
            try {
                System.out.println("正在同步学生数量...");
                classService.syncAllStudentCounts();
                System.out.println("学生数量同步完成！");
            } catch (Exception e) {
                System.err.println("同步学生数量失败: " + e.getMessage());
            }
        };
    }
}
