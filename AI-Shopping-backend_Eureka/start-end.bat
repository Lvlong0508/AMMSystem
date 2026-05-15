@echo off

set "BASE_DIR=%~dp0"

echo Starting services...

start "eureka-server" /D "%BASE_DIR%eureka-server" cmd /c "mvn spring-boot:run"
timeout /t 3 /nobreak >nul

start "gateway-service" /D "%BASE_DIR%gateway-service" cmd /c "mvn spring-boot:run"
timeout /t 5 /nobreak >nul

start "auth-service" /D "%BASE_DIR%auth-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul

start "product-service" /D "%BASE_DIR%product-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "shop-service" /D "%BASE_DIR%shop-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "contact-service" /D "%BASE_DIR%contact-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "logistics-service" /D "%BASE_DIR%logistics-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "order-service" /D "%BASE_DIR%order-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "chat-service" /D "%BASE_DIR%chat-service" cmd /c "mvn spring-boot:run"

echo.
echo =========================================
echo 微服务启动顺序：
echo 1. Eureka Server (8761) - 服务注册中心
echo 2. Gateway Service (8080) - API网关
echo 3. Auth Service (8086) - 认证服务
echo 4. Product Service (8081) - 商品服务
echo 5. Shop Service (8087) - 店铺服务
echo 6. Contact Service (8083) - 联系人服务
echo 7. Logistics Service (8084) - 物流服务
echo 8. Order Service (8082) - 订单服务
echo 9. Chat Service (8085) - AI聊天服务
echo =========================================
echo.
echo 按 Q 停止所有服务，或按其他键退出（服务继续运行）
echo.

:wait_loop
choice /C QX /N /T 1 /D X >nul
if errorlevel 2 goto wait_loop
if errorlevel 1 goto stop_services

:stop_services
call "%~dp0stop-end.bat"