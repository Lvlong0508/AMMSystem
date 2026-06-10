@echo off
chcp 65001 >nul

echo =========================================
echo  AI-Shopping 后端停止脚本
echo =========================================
echo.
echo 正在关闭以下端口的服务：
echo.
echo - 8088  (Gateway Service)
echo - 8086  (Auth Service)
echo - 8081  (Product Service)
echo - 8087  (Shop Service)
echo - 8082  (Order Service)
echo - 8083  (Contact Service)
echo - 8084  (Logistics Service)
echo - 8085  (Chat Service)
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8088') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8086') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8081') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8087') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8082') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8083') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8084') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8085') do taskkill /F /PID %%a 2>nul

taskkill /F /IM java.exe 2>nul

echo.
echo 所有后端服务已停止。
echo Nacos 需单独停止（如需要）。
pause