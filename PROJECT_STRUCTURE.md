# 面向实验室的智能文件管理与共享平台 - 项目结构文档

## 1. 项目概述

本项目是一个基于双微服务架构的实验室文件管理与共享平台，采用Spring Boot、Spring Cloud、MySQL、Redis、MinIO等技术栈，实现文件全生命周期管理、大文件分片上传、精细化权限控制等功能。

## 2. 项目整体结构

```
lab-file-platform/
├── .mvn/                           # Maven Wrapper配置
│   └── wrapper/
│       ├── maven-wrapper.properties
│       └── MavenWrapperDownloader.java
├── common/                         # 公共模块
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── lab/
│   │                   └── file/
│   │                       └── common/
│   │                           ├── config/           # 公共配置
│   │                           │   └── MybatisPlusConfig.java
│   │                           ├── exception/        # 异常处理
│   │                           │   └── BusinessException.java
│   │                           ├── response/         # 统一响应
│   │                           │   └── Result.java
│   │                           └── utils/            # 工具类
│   │                               └── JwtUtil.java
│   └── pom.xml
├── user-service/                   # 用户服务模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── lab/
│   │   │   │           └── file/
│   │   │   │               └── userservice/
│   │   │   │                   ├── UserServiceApplication.java  # 启动类
│   │   │   │                   ├── config/           # 配置类
│   │   │   │                   │   └── SecurityConfig.java
│   │   │   │                   ├── controller/       # 控制器
│   │   │   │                   │   └── UserController.java
│   │   │   │                   ├── entity/           # 实体类
│   │   │   │                   │   └── User.java
│   │   │   │                   ├── mapper/           # 数据访问层
│   │   │   │                   │   └── UserMapper.java
│   │   │   │                   ├── service/          # 业务逻辑层
│   │   │   │                   │   ├── IUserService.java
│   │   │   │                   │   └── impl/
│   │   │   │                   │       └── UserServiceImpl.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── lab/
│   │                   └── file/
│   │                       └── userservice/
│   │                           └── UserServiceTest.java
│   └── pom.xml
├── file-service/                   # 文件服务模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── lab/
│   │   │   │           └── file/
│   │   │   │               └── fileservice/
│   │   │   │                   ├── FileServiceApplication.java  # 启动类
│   │   │   │                   ├── config/           # 配置类
│   │   │   │                   │   └── MinioConfig.java
│   │   │   │                   ├── controller/       # 控制器
│   │   │   │                   │   └── FileController.java
│   │   │   │                   ├── entity/           # 实体类
│   │   │   │                   │   ├── FileEntity.java
│   │   │   │                   │   ├── FileShare.java
│   │   │   │                   │   ├── OperationLog.java
│   │   │   │                   │   └── FileChunk.java
│   │   │   │                   ├── mapper/           # 数据访问层
│   │   │   │                   │   ├── FileMapper.java
│   │   │   │                   │   ├── FileShareMapper.java
│   │   │   │                   │   ├── OperationLogMapper.java
│   │   │   │                   │   └── FileChunkMapper.java
│   │   │   │                   ├── service/          # 业务逻辑层
│   │   │   │                   │   ├── IFileService.java
│   │   │   │                   │   ├── IFileShareService.java
│   │   │   │                   │   ├── IOperationLogService.java
│   │   │   │                   │   ├── IFileChunkService.java
│   │   │   │                   │   └── impl/
│   │   │   │                   │       ├── FileServiceImpl.java
│   │   │   │                   │       ├── FileShareServiceImpl.java
│   │   │   │                   │       ├── OperationLogServiceImpl.java
│   │   │   │                   │       └── FileChunkServiceImpl.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   └── pom.xml
├── gateway/                        # API网关模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── lab/
│   │   │   │           └── file/
│   │   │   │               └── gateway/
│   │   │   │                   ├── GatewayApplication.java  # 启动类
│   │   │   │                   ├── config/           # 网关配置
│   │   │   │                   │   └── GatewayConfig.java
│   │   │   │                   └── filter/           # 过滤器
│   │   │   │                       └── AuthGlobalFilter.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   └── pom.xml
├── doc/                            # 文档目录
│   └── init_database.sql           # 数据库初始化脚本
├── .gitignore                      # Git忽略文件配置
├── docker-compose.yml              # Docker容器编排配置
├── Dockerfile.*                    # Docker镜像构建文件
├── deploy.bat                      # Windows部署脚本
├── mvnw                            # Unix/Linux Maven Wrapper脚本
├── mvnw.cmd                        # Windows Maven Wrapper脚本
├── pom.xml                         # 项目总配置文件
├── README.md                       # 项目说明文档
├── PROJECT_SUMMARY.md              # 项目总结报告
├── DEPLOY_GUIDE.md                 # 部署指南
├── API_DOC.md                      # API文档
└── LICENSE                         # 项目许可证
```

## 3. 模块详细说明

### 3.1 common 模块
**职责**: 提供各服务共享的组件和工具

**包含内容**:
- **exception**: 自定义业务异常处理
- **response**: 统一响应结果封装
- **config**: 公共配置类(MyBatis-Plus配置)
- **utils**: 工具类(JWT工具)

### 3.2 user-service 模块
**职责**: 用户管理、权限认证、用户信息维护

**主要组件**:
- **entity.User**: 用户实体类，包含用户基本信息和权限
- **mapper.UserMapper**: 用户数据访问接口
- **service.IUserService**: 用户业务接口定义
- **service.impl.UserServiceImpl**: 用户业务逻辑实现
- **controller.UserController**: 用户相关API接口
- **config.SecurityConfig**: 安全配置类

### 3.3 file-service 模块
**职责**: 文件管理、文件存储、文件分享、操作审计

**主要组件**:
- **entity**:
  - FileEntity: 文件实体
  - FileShare: 文件分享实体
  - OperationLog: 操作日志实体
  - FileChunk: 文件分片实体
- **mapper**: 各实体对应的数据库访问接口
- **service**: 文件相关的业务接口定义
- **service.impl**: 文件业务逻辑实现
- **controller.FileController**: 文件相关API接口
- **config.MinioConfig**: MinIO对象存储配置

### 3.4 gateway 模块
**职责**: API统一入口、认证鉴权、路由转发、流量控制

**主要组件**:
- **config.GatewayConfig**: 网关路由配置
- **filter.AuthGlobalFilter**: JWT认证过滤器

## 4. 技术架构层次

### 4.1 表现层 (Presentation Layer)
- **网关层**: Spring Cloud Gateway - 统一入口、路由、鉴权
- **控制层**: Spring MVC - RESTful API实现

### 4.2 业务逻辑层 (Business Logic Layer)
- **服务层**: Spring Service - 业务逻辑处理
- **安全层**: Spring Security - 认证授权

### 4.3 数据访问层 (Data Access Layer)
- **ORM层**: MyBatis-Plus - 数据库操作封装
- **连接池**: HikariCP - 数据库连接管理

### 4.4 数据存储层 (Data Storage Layer)
- **关系型数据库**: MySQL - 结构化数据存储
- **缓存数据库**: Redis - 会话、热点数据缓存
- **对象存储**: MinIO - 文件存储

### 4.5 基础设施层 (Infrastructure Layer)
- **服务注册中心**: Nacos - 服务发现与配置管理
- **负载均衡**: Ribbon/LoadBalancer - 服务调用负载均衡

## 5. 依赖关系

```
    gateway
       ↓ (服务调用)
user-service ←→ common
                  ↑
file-service ←─────┘
```

- **gateway** 依赖于 **user-service** 和 **file-service**
- **user-service** 和 **file-service** 都依赖于 **common**
- 所有模块通过Nacos进行服务注册与发现

## 6. 配置文件结构

### 6.1 全局配置 (pom.xml)
- 依赖版本管理
- 插件配置
- 模块定义

### 6.2 各服务配置 (application.yml)
- 服务名称、端口
- 数据库连接信息
- Redis连接信息
- MinIO配置
- Nacos注册中心地址

## 7. 测试结构

每个服务都包含相应的单元测试，使用JUnit 5进行测试验证，确保各模块功能正确性。

## 8. 部署相关

- **Docker相关**: docker-compose.yml, Dockerfile.*
- **构建脚本**: mvnw*, deploy.bat
- **文档**: 部署指南、API文档