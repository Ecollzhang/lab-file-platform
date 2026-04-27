# 部署指南

## 环境准备

### 1. 安装基础软件
- Java 11+
- Maven 3.6+
- MySQL 8.0
- Redis
- MinIO

### 2. 启动依赖服务

#### Nacos (服务注册中心)
1. 下载Nacos: https://github.com/alibaba/nacos/releases
2. 解压并启动:
```bash
# Linux/Mac
bin/startup.sh -m standalone

# Windows
bin\startup.cmd -m standalone
```

#### MySQL (数据库)
1. 安装MySQL 8.0
2. 执行初始化脚本 `doc/init_database.sql`
3. 验证数据库连接

#### Redis (缓存)
1. 安装Redis
2. 启动Redis服务

#### MinIO (对象存储)
1. 下载MinIO: https://min.io/download
2. 启动MinIO服务:
```bash
minio server /data --console-address ":9001"
```

## 项目构建

### 1. 克隆项目
```bash
git clone <your-repo-url>
cd lab-file-platform
```

### 2. 修改配置文件
根据你的环境修改各服务的 `application.yml`:
- 数据库连接信息
- Redis连接信息
- MinIO连接信息
- Nacos地址

### 3. 编译打包
```bash
mvn clean package -DskipTests
```

## 服务启动顺序

### 方法一: 依次启动 (推荐用于开发环境)

1. **启动用户服务**
```bash
java -jar user-service/target/user-service.jar
```

2. **启动文件服务**
```bash
java -jar file-service/target/file-service.jar
```

3. **启动网关服务**
```bash
java -jar gateway/target/gateway.jar
```

### 方法二: 使用Docker Compose (推荐用于生产环境)

1. 构建Docker镜像
```bash
mvn clean package -DskipTests
```

2. 启动所有服务
```bash
docker-compose up -d
```

## 验证部署

### 1. 检查服务注册
访问Nacos控制台: http://localhost:8848/nacos
用户名/密码: nacos/nacos
确认 user-service 和 file-service 已注册

### 2. 测试API接口
```bash
# 测试用户服务
curl -X POST http://localhost:8080/api/user/login \
  -d "username=admin&password=123456"

# 测试文件服务
curl -X GET http://localhost:8080/api/file/list?userId=1&parentId=0
```

## 默认账号

- **管理员**: admin / 123456
- **导师**: teacher / 123456
- **研究生**: student / 123456

## 监控和运维

### 1. 服务监控
- Nacos控制台: 服务状态监控
- 应用日志: 查看各服务的日志输出
- 数据库监控: 监控数据库连接和性能

### 2. 常见问题排查

#### 服务无法注册到Nacos
- 检查网络连接
- 检查Nacos地址配置
- 检查防火墙设置

#### 数据库连接失败
- 检查数据库服务是否启动
- 检查连接参数
- 检查数据库用户权限

#### 文件上传失败
- 检查MinIO服务状态
- 检查存储路径权限
- 检查文件大小限制配置

## 生产环境部署建议

1. **使用反向代理**
   - 配置Nginx作为反向代理
   - 启用SSL证书
   - 配置负载均衡

2. **数据备份策略**
   - 定期备份MySQL数据库
   - 备份Redis数据
   - 备份MinIO存储的数据

3. **监控告警**
   - 集成Prometheus + Grafana
   - 配置日志收集系统
   - 设置关键指标告警

4. **安全加固**
   - 使用HTTPS
   - 配置防火墙规则
   - 定期更新依赖包
   - 启用访问控制