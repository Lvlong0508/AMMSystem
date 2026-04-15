@echo off
chcp 65001 >nul
echo ==========================================
echo     AI-Shopping 微服务启动脚本
echo ==========================================
echo.

setlocal EnableDelayedExpansion

set "BASE_DIR=%~dp0"
set "MAX_WAIT_SECONDS=60"
set "CHECK_INTERVAL=2"
set "TEMP_FILE=%TEMP%\health_check.json"

:: 错误处理：检测到启动失败时退出
call :start_and_check "eureka-server" "Eureka Server" "http://localhost:8761/actuator/health" 8761
if errorlevel 1 exit /b 1

call :start_and_check "product-service" "Product Service" "http://localhost:8081/actuator/health" 8081
if errorlevel 1 exit /b 1

call :start_and_check "contact-service" "Contact Service" "http://localhost:8082/actuator/health" 8082
if errorlevel 1 exit /b 1

call :start_and_check "logistics-service" "Logistics Service" "http://localhost:8084/actuator/health" 8084
if errorlevel 1 exit /b 1

call :start_and_check "order-service" "Order Service" "http://localhost:8083/actuator/health" 8083
if errorlevel 1 exit /b 1

call :start_and_check "chat-service" "Chat Service" "http://localhost:8085/actuator/health" 8085
if errorlevel 1 exit /b 1

echo.
echo ==========================================
echo     所有服务启动成功！
echo ==========================================
echo.
echo Eureka 控制台: http://localhost:8761
echo.
pause
exit /b 0

:: 启动并检查服务是否启动成功
:start_and_check
set "SERVICE_NAME=%~1"
set "SERVICE_TITLE=%~2"
set "HEALTH_URL=%~3"
set "PORT=%~4"

echo [%SERVICE_NAME%] 正在启动 %SERVICE_TITLE%...

:: 检查端口是否被占用
netstat -an | findstr ":%PORT%" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [%SERVICE_NAME%] 错误：端口 %PORT% 已被占用，请先停止占用该端口的服务！
    call :stop_all
    exit /b 1
)

:: 启动服务 - 使用pushd简化路径处理
start "%SERVICE_TITLE%" cmd /c "pushd %BASE_DIR%%SERVICE_NAME% && mvn spring-boot:run"

:: 等待并检查健康状态
set /a waited=0
echo [%SERVICE_NAME%] 等待服务启动中...

:wait_loop
if !waited! GEQ %MAX_WAIT_SECONDS% (
    echo [%SERVICE_NAME%] 错误：服务启动超时（%MAX_WAIT_SECONDS%秒），启动失败！
    call :stop_all
    exit /b 1
)

timeout /t %CHECK_INTERVAL% /nobreak >nul
set /a waited+=CHECK_INTERVAL

:: 尝试连接健康检查端点，并保存响应到临时文件
curl -s --connect-timeout 2 "%HEALTH_URL%" -o "%TEMP_FILE%" 2>nul
if errorlevel 1 (
    echo [%SERVICE_NAME%] 健康检查中...（已等待!waited!秒）
    goto wait_loop
)

:: 检查JSON响应中是否包含 "UP"
findstr /C:"\"UP\"" "%TEMP_FILE%" >nul 2>&1
if errorlevel 1 (
    echo [%SERVICE_NAME%] 健康检查中...（已等待!waited!秒，服务未就绪）
    goto wait_loop
)

:: 清理临时文件
del "%TEMP_FILE%" 2>nul

echo [%SERVICE_NAME%] 服务启动成功！（耗时!waited!秒）
exit /b 0

:: 启动失败时关闭所有已启动的服务
:stop_all
echo.
echo 正在回滚，关闭所有已启动的服务...
call "%~dp0stop-all.bat"
if exist "%TEMP_FILE%" del "%TEMP_FILE%" 2>nul
exit /b 0
