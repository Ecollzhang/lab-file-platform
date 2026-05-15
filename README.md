# 面向实验室的智能文件管理与共享平台

## 项目概述

基于**双微服务架构**+**前端 Vue 3** 的全栈实验室文件管理与共享平台。实现文件全生命周期管理、大文件分片上传秒传、精细化权限控制、文件定向分享、操作日志审计等功能。系统通过网关统一入口与负载均衡提升可用性，结合 Redis（RDB+AOF 双持久化）做缓存与限流，MinIO 做分布式文件存储，满足实验室科研数据安全管理、师生协同与报告提交的实际业务需求。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.18, Spring Cloud 2021.0.8
- **微服务治理**: Spring Cloud Alibaba Nacos (服务注册发现)
- **网关**: Spring Cloud Gateway (全局 JWT 鉴权 + 限流)
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus 3.5.3.2
- **缓存**: Redis 6.2 (RDB + AOF 双持久化)
- **文件存储**: MinIO (S3 兼容对象存储)
- **安全**: JWT (jjwt 0.11.5) + BCrypt 密码加密
- **限流**: Redis 滑动窗口限流器
- **构建**: Maven (Java 11)

### 前端
- **框架**: Vue 3 (Composition API)
- **构建工具**: Vite 5
- **UI 组件库**: Element Plus 2.6
- **状态管理**: Pinia (持久化存储)
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **文件校验**: SparkMD5 (大文件分片秒传)

## 架构设计

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   前端 Vue 3 (端口 5173)                  │
│           ├── 我的文件 ├── 我的分享 ├── 与我共享            │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / Axios (Bearer Token)
┌──────────────────────▼──────────────────────────────────┐
│             API 网关 (端口 8080/8083)                     │
│          Spring Cloud Gateway + JWT 鉴权                 │
│          滑动窗口限流 + Redis Token 校验                  │
└────┬─────────────────┬──────────────────┬───────────────┘
     │                 │                  │
┌────▼────────┐ ┌─────▼─────────┐  ┌────▼────────────┐
│ 用户服务     │ │  文件服务      │  │    Nacos        │
│ (端口 8081) │ │  (端口 8082)   │  │ 服务注册发现     │
│            │ │               │  │  (端口 8848)    │
└────┬────────┘ └─────┬─────────┘  └─────────────────┘
     │                │
     ▼                ▼
┌─────────────────────────────────────────────────────────┐
│                  基础设施层                               │
│  MySQL 8.0 │ Redis 6.2 │ MinIO 对象存储 │ Nacos          │
│            │ (持久化)   │               │                │
└─────────────────────────────────────────────────────────┘
```

### 微服务拆分

| 模块 | 端口 | 职责 |
|------|------|------|
| **common** | - | 公共模块：统一响应、JWT工具、Redis服务、限流器、全局异常处理 |
| **user-service** | 8081 | 用户认证、注册登录、个人信息管理、角色权限 |
| **file-service** | 8082 | 文件上传下载、文件夹管理、分片上传、文件分享、操作日志 |
| **gateway** | 8080/8083 | 统一入口、JWT鉴权、Redis Token校验、滑动窗口限流、路由转发 |
| **frontend** | 5173 | Vue 3 单页应用，代理 API 到网关 |

## 项目结构

```
lab-file-platform/
├── frontend/                      # Vue 3 前端
│   ├── src/
│   │   ├── views/                 # 页面组件
│   │   │   ├── Login.vue          # 登录
│   │   │   ├── Register.vue       # 注册
│   │   │   ├── MyFiles.vue        # 我的文件（核心）
│   │   │   ├── ShareManage.vue    # 我的分享管理
│   │   │   ├── SharedWithMe.vue   # 与我共享
│   │   │   ├── ShareView.vue      # 分享码查看
│   │   │   └── Profile.vue        # 个人中心
│   │   ├── components/            # 可复用组件
│   │   │   ├── AppLayout.vue      # 主布局（侧边栏+顶栏）
│   │   │   ├── UploadDialog.vue   # 上传对话框（支持分片）
│   │   │   ├── FilePreview.vue    # 文件预览
│   │   │   ├── ShareDialog.vue    # 创建分享
│   │   │   ├── MoveDialog.vue     # 移动文件（树选择）
│   │   │   ├── RenameDialog.vue   # 重命名
│   │   │   ├── CreateFolderDialog.vue
│   │   │   └── BreadcrumbNav.vue  # 面包屑导航
│   │   ├── stores/user.js         # Pinia 用户状态
│   │   ├── router/index.js        # 路由（含登录守卫）
│   │   └── utils/
│   │       ├── request.js         # Axios 封装
│   │       └── api.js             # 所有 API 接口
│   ├── vite.config.js
│   └── package.json
├── common/                        # 公共模块
│   └── src/main/java/com/lab/file/common/
│       ├── config/                # RedisConfig, MybatisPlusConfig
│       ├── exception/             # BusinessException
│       ├── handler/               # GlobalExceptionHandler
│       ├── response/              # Result<T> 统一响应
│       ├── service/               # TokenRedisService, SlidingWindowRateLimiter
│       ├── utils/                 # JwtUtil
│       └── vo/                    # LoginUserInfo
├── user-service/                  # 用户服务
│   └── src/main/java/com/lab/file/userservice/
│       ├── config/                # SecurityConfig, OpenApiConfig
│       ├── controller/            # UserController
│       ├── entity/                # User
│       ├── mapper/                # UserMapper
│       └── service/impl/          # UserServiceImpl
├── file-service/                  # 文件服务
│   └── src/main/java/com/lab/file/fileservice/
│       ├── config/                # MinioConfig, OpenApiConfig
│       ├── controller/            # FileController, FileChunkController, FileShareController
│       ├── entity/                # FileEntity, FileChunk, FileShare, OperationLog
│       ├── mapper/                # FileMapper, FileChunkMapper, FileShareMapper
│       └── service/impl/          # FileServiceImpl, FileChunkServiceImpl, FileShareServiceImpl
├── gateway/                       # API 网关
│   └── src/main/java/com/lab/file/gateway/
│       └── filter/                # AuthGlobalFilter (JWT), RateLimitFilter
├── redis/conf/                    # Redis 持久化配置
│   └── redis.conf                 # RDB + AOF 双持久化
├── doc/                           # 数据库文档
│   ├── init_database.sql          # 完整建表 + 种子数据
│   └── upgrade_t_file_chunk.sql   # 分片上传表升级
├── docker-compose.yml             # 全栈容器化部署
├── deploy.bat                     # Windows 构建脚本
├── start.sh                       # Linux 启动脚本
└── pom.xml                        # Maven 父工程
```

## 核心功能

### 1. 用户权限管理
- 三种角色：**管理员** / **导师** / **研究生**，权限隔离
- 基于 JWT + Redis Token 双重认证
- BCrypt 密码加密存储
- 用户信息管理（邮箱、手机号）
- Token 自动续期（24小时有效期）

### 2. 文件管理
- **文件夹树形结构**：支持多级目录嵌套
- **文件操作**：上传、下载、预览、重命名、移动、删除（软删除）
- **文件预览**：图片 / PDF / 视频 / 音频 / 文本（通过 blob URL 鉴权流预览）
- **文件图标**：按扩展名自动匹配对应的图标和颜色

### 3. 大文件分片上传
- **分片上传**：5MB/片，`spark-md5` 计算文件 MD5
- **断点续传**：查询已上传分片，只传缺失部分
- **秒传**：MD5 匹配直接复用已有文件
- **失败重试**：单个分片最多重试 3 次
- **上传进度**：实时进度条展示

### 4. 文件共享
- **公开分享**：任何人通过分享码可访问
- **私密分享**：仅指定用户可访问
- **分享码**：8 位随机字符码
- **有效期控制**：支持按小时设置过期
- **下载次数限制**：可设置最大下载次数
- **共享目录浏览**：支持多级目录

### 5. 操作审计
- 记录所有文件操作（上传、删除、分享等）
- 记录操作人、IP、User-Agent、时间
- 操作日志持久化到数据库

### 6. 安全防护
- **网关层鉴权**：全部 API 统一 JWT 校验
- **Redis Token 校验**：登出即失效，防止 Token 泄露
- **限流**：Redis 滑动窗口算法防刷
- **防 SQL 注入**：MyBatis-Plus 参数化查询
- **用户隔离**：只能操作自己的文件

## API 接口

> 所有 API 通过网关统一入口 `http://localhost:8080/api/` 访问，需在 Header 中携带 `Authorization: Bearer <token>`。

### 用户服务 (`/api/user/**`)

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/user/login` | 用户登录，返回 JWT Token | 白名单 |
| POST | `/api/user/register` | 用户注册 | 白名单 |
| GET | `/api/user/{id}` | 获取用户信息 | 需登录 |
| PUT | `/api/user/{id}` | 更新用户信息 | 需登录 |
| POST | `/api/user/logout` | 登出，清除 Redis Token | 需登录 |

### 文件管理 (`/api/file/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/file/upload` | 上传文件到指定目录 |
| POST | `/api/file/folder` | 创建文件夹 |
| GET | `/api/file/list` | 获取指定目录下的文件列表 |
| DELETE | `/api/file/{fileId}` | 软删除文件/文件夹 |
| PUT | `/api/file/{fileId}/rename` | 重命名 |
| PUT | `/api/file/{fileId}/move` | 移动到其他目录 |
| GET | `/api/file/{fileId}/download` | 下载文件 |
| GET | `/api/file/{fileId}/preview` | 预览文件（内联显示） |

### 分片上传 (`/api/file/chunk/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/file/chunk/init` | 初始化分片上传，获取 uploadId |
| POST | `/api/file/chunk/upload` | 上传单个分片 |
| GET | `/api/file/chunk/uploaded` | 查询已上传分片 |
| GET | `/api/file/chunk/missing-chunks` | 查询缺失分片序号 |
| POST | `/api/file/chunk/merge` | 合并分片为完整文件 |
| POST | `/api/file/chunk/cancel` | 取消分片上传 |
| GET | `/api/file/chunk/exists` | 根据 MD5 检查文件是否存在（秒传） |

### 文件分享 (`/api/share/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/share/users` | 获取所有启用用户列表（分享选人用） |
| POST | `/api/share/create` | 创建分享（公开/私密） |
| GET | `/api/share/list` | 获取当前用户的分享记录 |
| GET | `/api/share/{shareCode}` | 根据分享码获取分享信息 |
| GET | `/api/share/{shareCode}/target-users` | 获取私密分享的目标用户 |
| GET | `/api/share/{shareCode}/download` | 通过分享码下载文件 |
| GET | `/api/share/{shareCode}/preview` | 通过分享码预览文件 |
| GET | `/api/share/{shareCode}/list` | 查看分享目录的文件列表 |
| POST | `/api/share/{shareCode}/upload` | 上传到分享目录 |
| DELETE | `/api/share/{shareId}` | 取消分享 |

## 数据模型

### t_user — 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| role | INT | 1=管理员 2=导师 3=研究生 |
| avatar | VARCHAR(255) | 头像 |
| status | INT | 0=禁用 1=启用 |

### t_file — 文件表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| original_name | VARCHAR(255) | 原始文件名 |
| file_name | VARCHAR(255) | UUID 存储文件名 |
| file_size | BIGINT | 文件大小（字节） |
| file_type | VARCHAR(100) | MIME 类型 |
| file_extension | VARCHAR(10) | 扩展名 |
| file_md5 | VARCHAR(32) | MD5 值 |
| user_id | BIGINT | 所属用户 |
| parent_id | BIGINT | 父目录（0=根目录） |
| is_directory | TINYINT | 是否文件夹 |
| status | INT | 0=删除 1=正常 |

### t_file_share — 分享表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_id | BIGINT | 分享的文件 |
| share_user_id | BIGINT | 分享者 |
| share_code | VARCHAR(20) | 8位分享码（唯一） |
| share_type | INT | 1=公开 2=私密 |
| expire_time | DATETIME | 过期时间 |
| max_download_count | INT | 最大下载次数（0=不限） |
| status | INT | 0=失效 1=有效 |

## 快速开始

### Docker 部署（推荐）

```bash
# 1. 编译所有微服务
mvn clean package -DskipTests

# 2. 启动全栈服务
docker-compose up -d

# 3. 启动前端
cd frontend
npm install
npm run dev
```

访问：
- **前端页面**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **Nacos 控制台**: http://localhost:8848/nacos
- **MinIO 控制台**: http://localhost:9001

### 本地手动部署

1. **环境准备**: Java 11+, Maven, MySQL 8.0, Redis 6.2, MinIO, Nacos
2. **初始化数据库**: 执行 `doc/init_database.sql`
3. **启动中间件**: Nacos → MySQL → Redis → MinIO
4. **修改配置**: 各模块 `application.yml` 中的数据库、Redis、MinIO 连接信息
5. **构建运行**:

```bash
# 后端
mvn clean package -DskipTests
java -jar user-service/target/user-service.jar &
java -jar file-service/target/file-service.jar &
java -jar gateway/target/gateway.jar &

# 前端
cd frontend
npm install
npm run dev
```

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 导师 | teacher | 123456 |
| 研究生 | student | 123456 |

## Redis 持久化说明

系统使用 Redis 存储 JWT Token（24h 过期），为保证宕机后不丢失登录态，已配置 **RDB + AOF 双持久化**：

- **RDB 快照**：周期性全量备份（最久 15 分钟间隔）
- **AOF 日志**：每秒 fsync（最多丢失 1 秒数据）
- **混合模式**：AOF 重写时使用 RDB 格式作为前缀，保证快速加载
- **数据目录**：持久化文件存储在 Docker volume `redis_data:/data`

配置文件: `redis/conf/redis.conf`

## 选型说明

| 技术 | 选型理由 |
|------|----------|
| **Spring Cloud Gateway** | 非阻塞网关，集成 Nacos 服务发现，自带过滤器链适合做统一鉴权限流 |
| **MyBatis-Plus** | 代码生成、分页、逻辑删除等开箱即用，减少大量重复 SQL |
| **MinIO** | S3 兼容、轻量级、支持分片上传、支持断点续传，适合实验室私有部署 |
| **Redis** | 内存数据库，适合存储 Token 做快速校验，支持持久化 |
| **JWT** | 无状态认证，减少数据库查询，配合 Redis 做黑名单控制 |
| **Vue 3 + Element Plus** | 响应式前端框架，Element Plus 组件库完善，适合后台管理类应用 |

## 安全特性

1. JWT + Redis 双 Token 校验（网关层统一拦截）
2. BCrypt 密码加密
3. Redis 滑动窗口限流防刷
4. 软删除保护（`status=0` 而非物理删除）
5. 用户数据隔离（只能操作自己的文件）
6. 分享权限控制（公开/私密+过期时间+下载次数）
7. 操作日志审计追溯

## 性能优化

1. Redis 缓存热点 Token 数据
2. 文件分片上传（5MB/片）减少大文件内存占用
3. 文件秒传（MD5 匹配避免重复上传）
4. 连接池配置优化（HikariCP + Jedis Pool）
5. 异步处理操作日志
6. 前端代码分割（Vite 懒加载路由）
