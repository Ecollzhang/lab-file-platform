package com.lab.file.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API文档配置 - 用户服务
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("用户服务API文档")
                        .description("面向实验室的智能文件管理与共享平台 - 用户服务API接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("实验室文件管理系统")
                                .email("support@lab-file-platform.com")));
    }
}