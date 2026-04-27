#!/bin/bash
# lab-file-platform 启动脚本

echo "==========================================="
echo "  面向实验室的智能文件管理与共享平台"
echo "  一键启动脚本"
echo "==========================================="

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请先安装Java 11+"
    exit 1
fi

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请先安装Maven"
    exit 1
fi

echo "检查并编译项目..."

# 编译项目
if ! mvn clean compile -q; then
    echo "编译失败，请检查代码错误"
    exit 1
fi

echo "编译完成！"

# 检查是否有打包好的jar文件
if [ ! -f "user-service/target/user-service.jar" ] || [ ! -f "file-service/target/file-service.jar" ] || [ ! -f "gateway/target/gateway.jar" ]; then
    echo "未找到打包好的JAR文件，正在打包..."
    if ! mvn package -DskipTests -q; then
        echo "打包失败"
        exit 1
    fi
    echo "打包完成！"
fi

echo ""
echo "请确保以下服务已启动："
echo "1. Nacos (端口 8848) - 服务注册中心"
echo "2. MySQL (端口 3306) - 数据库"
echo "3. Redis (端口 6379) - 缓存"
echo "4. MinIO (端口 9000) - 对象存储"
echo ""

read -p "是否继续启动服务？(y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "启动用户服务..."
    nohup java -jar user-service/target/user-service.jar > user-service.log 2>&1 &
    USER_PID=$!
    echo "用户服务PID: $USER_PID"

    echo "等待用户服务启动..."
    sleep 10

    echo "启动文件服务..."
    nohup java -jar file-service/target/file-service.jar > file-service.log 2>&1 &
    FILE_PID=$!
    echo "文件服务PID: $FILE_PID"

    echo "等待文件服务启动..."
    sleep 10

    echo "启动网关服务..."
    nohup java -jar gateway/target/gateway.jar > gateway.log 2>&1 &
    GATEWAY_PID=$!
    echo "网关服务PID: $GATEWAY_PID"

    echo ""
    echo "==========================================="
    echo "  服务启动完成！"
    echo "==========================================="
    echo "网关地址: http://localhost:8080"
    echo "用户服务: http://localhost:8081"
    echo "文件服务: http://localhost:8082"
    echo "Nacos控制台: http://localhost:8848/nacos"
    echo "用户名/密码: nacos/nacos"
    echo ""
    echo "日志文件:"
    echo "  - user-service.log"
    echo "  - file-service.log"
    echo "  - gateway.log"
    echo ""
    echo "默认账号:"
    echo "  - 管理员: admin / 123456"
    echo "  - 导师: teacher / 123456"
    echo "  - 研究生: student / 123456"
    echo ""
else
    echo "已取消启动"
fi