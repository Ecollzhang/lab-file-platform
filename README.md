# 面向实验室的智能文件管理与共享平台

## 项目概述

该系统基于双微服务架构设计，实现实验室文件全生命周期管理、大文件分片上传、精细化权限控制、文件定向发送与共享、操作日志审计等功能。系统通过网关统一入口与负载均衡提升可用性，结合 Redis 做缓存与限流，MinIO 做分布式文件存储，满足实验室科研数据安全管理、师生协同与报告提交的实际业务需求。

## 技术栈

- **后端框架**: Spring Boot 2.7.0, Spring Cloud 2021.0.3
- **微服务治理**: Spring Cloud Alibaba Nacos (服务注册发现与配置中心)
- **网关**: Spring Cloud Gateway
- **数据库**: MySQL 8.0
- **ORM框架**: MyBatis-Plus 3.5.2
- **缓存**: Redis
- **文件存储**: MinIO 分布式存储
- **安全框架**: Spring Security + JWT
- **构建工具**: Maven

## 架构设计

### 微服务拆分

项目采用双微服务架构：
- **user-service**: 用户权限服务，负责用户认证、权限管理等
- **file-service**: 文件服务，负责文件上传下载、分片管理、分享等
- **gateway**: API网关，统一入口、鉴权、限流等

### 核心功能

#### 1. 用户权限管理
- 支持管理员、导师、研究生多角色权限隔离
- 基于JWT的用户认证机制
- 角色权限精细控制

#### 2. 文件管理
- 文件夹管理
- 文件上传/下载/预览
- 文件版本控制
- 回收站功能

#### 3. 大文件分片上传
- 支持大文件分片上传
- 断点续传功能
- 文件秒传功能

#### 4. 文件共享
- 文件定向发送
- 短链分享
- 权限控制（公开/私密分享）
- 有效期控制

#### 5. 操作审计
- 操作日志记录
- 文件访问统计
- 安全审计

#### 6. 性能优化
- Redis缓存热点数据
- 数据库查询优化
- 分布式文件存储

## 项目结构

```
lab-file-platform/
├── common/                 # 公共模块
│   ├── exception/          # 异常处理
│   ├── response/           # 统一响应
│   ├── config/             # 公共配置
│   └── utils/              # 工具类
├── user-service/          # 用户服务
│   ├── entity/            # 实体类
│   ├── mapper/            # 数据访问层
│   ├── service/           # 业务逻辑层
│   ├── controller/        # 控制器层
│   └── config/            # 配置类
├── file-service/          # 文件服务
│   ├── entity/            # 实体类
│   ├── mapper/            # 数据访问层
│   ├── service/           # 业务逻辑层
│   ├── controller/        # 控制器层
│   └── config/            # 配置类
├── gateway/               # API网关
│   ├── filter/            # 过滤器
│   └── config/            # 配置类
├── doc/                   # 文档
└── pom.xml                # 项目总配置
```

## 快速开始

### 环境准备

1. **安装 Java 11+**
2. **安装 Maven**
3. **安装 MySQL 8.0**
4. **安装 Redis**
5. **安装 MinIO**
6. **安装 Nacos**

### 部署步骤

1. **初始化数据库**
   ```sql
   # 执行 doc/init_database.sql 脚本创建数据库和表
   ```

2. **启动中间件**
   ```bash
   # 启动 Nacos (默认端口 8848)
   # 启动 Redis (默认端口 6379)
   # 启动 MinIO (默认端口 9000)
   # 启动 MySQL
   ```

3. **配置文件修改**
   根据实际情况修改各模块的 `application.yml` 中的数据库连接、Redis、MinIO 等配置。

4. **编译打包**
   ```bash
   mvn clean package
   ```

5. **启动服务**
   ```bash
   # 启动用户服务
   java -jar user-service.jar
   
   # 启动文件服务
   java -jar file-service.jar
   
   # 启动网关服务
   java -jar gateway.jar
   ```

### 默认账号

- **管理员**: admin / 123456
- **导师**: teacher / 123456
- **研究生**: student / 123456

## API 接口

### 用户服务

- `POST /api/user/login` - 用户登录
- `POST /api/user/register` - 用户注册
- `GET /api/user/{id}` - 获取用户信息

### 文件服务

- `POST /api/file/upload` - 上传文件
- `POST /api/file/folder` - 创建文件夹
- `GET /api/file/list` - 获取文件列表
- `DELETE /api/file/{fileId}` - 删除文件
- `PUT /api/file/{fileId}/rename` - 重命名文件
- `PUT /api/file/{fileId}/move` - 移动文件
- `GET /api/file/{fileId}/download` - 下载文件

## 安全特性

1. **JWT Token 认证**
2. **细粒度权限控制**
3. **文件访问权限验证**
4. **操作日志审计**
5. **SQL注入防护**
6. **XSS攻击防护**

## 性能优化

1. **Redis 缓存热点数据**
2. **数据库查询优化**
3. **文件分片上传减少内存占用**
4. **连接池配置优化**
5. **异步处理提升响应速度**

## 扩展性设计

1. **微服务架构便于水平扩展**
2. **支持分布式部署**
3. **插件化设计便于功能扩展**
4. **预留接口便于二次开发**

## 部署建议

1. **生产环境建议使用 Docker 容器化部署**
2. **配合 Kubernetes 进行服务编排**
3. **使用负载均衡器提高可用性**
4. **定期备份数据和配置**
5. **监控服务健康状态**