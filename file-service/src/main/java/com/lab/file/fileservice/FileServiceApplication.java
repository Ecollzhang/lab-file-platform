package com.lab.file.fileservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 文件服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.lab.file.fileservice.mapper")
@ComponentScan(basePackages = {"com.lab.file.fileservice", "com.lab.file.common"})
public class FileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileServiceApplication.class, args);
    }
}