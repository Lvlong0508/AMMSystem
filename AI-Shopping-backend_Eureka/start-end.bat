@echo off
chcp 65001 >nul

set "BASE_DIR=%~dp0"
set "JAVA_HOME=F:\JDK\jdk 17.0.17"

echo =========================================
echo  AI-Shopping 后端启动脚本 (Nacos 版)
echo =========================================
echo.
echo 前置条件：请确保 Nacos Server 已启动
echo  Nacos 地址: 10.200.97.197:8848
echo.
echo 正在启动服务...
echo.

echo [1/8] 编译 common-api ...
cd /d "%BASE_DIR%"
call mvn clean install -pl common-api -Dmaven.test.skip=true -q
echo.

start "gateway-service" /D "%BASE_DIR%gateway-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 5 /nobreak >nul

start "auth-service" /D "%BASE_DIR%auth-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "product-service" /D "%BASE_DIR%product-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "shop-service" /D "%BASE_DIR%shop-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "contact-service" /D "%BASE_DIR%contact-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "logistics-service" /D "%BASE_DIR%logistics-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "order-service" /D "%BASE_DIR%order-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"
timeout /t 1 /nobreak >nul

start "chat-service" /D "%BASE_DIR%chat-service" cmd /c "mvn spring-boot:run -Dmaven.test.skip=true"

echo.
echo =========================================
echo  微服务启动顺序：
echo  1. (独立) Nacos Server (8848) - 注册中心
echo  2. Gateway Service (8088)     - API网关
echo  3. Auth Service (8086)        - 认证服务
echo  4. Product Service (8081)     - 商品服务
echo  5. Shop Service (8087)        - 店铺服务
echo  6. Contact Service (8083)     - 联系人服务
echo  7. Logistics Service (8084)   - 物流服务
echo  8. Order Service (8082)       - 订单服务
echo  9. Chat Service (8085)        - AI聊天服务
echo =========================================
echo  使用 JDK: %JAVA_HOME%
echo.
echo 按 Q 停止所有服务，或关闭窗口（服务继续运行）
echo.

:wait_loop
choice /C QX /N /T 1 /D X >nul
if errorlevel 2 goto wait_loop
if errorlevel 1 goto stop_services

:stop_services
call "%~dp0stop-end.bat"