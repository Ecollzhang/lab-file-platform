@echo off
echo.
echo ================================
echo 面向实验室的智能文件管理与共享平台
echo 部署脚本
echo ================================
echo.

REM 检查Maven是否安装
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven，请先安装Maven
    exit /b 1
)

echo 正在清理之前的构建...
call mvn clean

echo 正在编译和打包项目...
call mvn package -DskipTests

if %errorlevel% neq 0 (
    echo 构建失败
    exit /b 1
)

echo.
echo 项目构建成功!
echo.
echo 可执行文件位置:
echo   - 用户服务: user-service\target\user-service.jar
echo   - 文件服务: file-service\target\file-service.jar
echo   - 网关服务: gateway\target\gateway.jar
echo.

echo 启动服务示例:
echo   1. 启动Nacos (访问 http://localhost:8848/nacos, 用户名/密码: nacos/nacos)
echo   2. 启动MySQL数据库
echo   3. 启动Redis
echo   4. 启动MinIO
echo   5. java -jar user-service\target\user-service.jar
echo   6. java -jar file-service\target\file-service.jar
echo   7. java -jar gateway\target\gateway.jar
echo.

pause