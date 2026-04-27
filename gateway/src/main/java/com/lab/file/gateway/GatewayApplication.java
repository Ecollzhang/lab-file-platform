package com.lab.file.gateway;

import com.lab.file.common.handler.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 网关服务启动类
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        WebMvcAutoConfiguration.class
})
@EnableDiscoveryClient
@ComponentScan(
        basePackages = {"com.lab.file.gateway", "com.lab.file.common"},
        // 关键：在这里排除 GlobalExceptionHandler
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        ))
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}