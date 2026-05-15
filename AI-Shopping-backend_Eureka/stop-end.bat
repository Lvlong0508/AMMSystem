@echo off

echo Stopping services...
echo.
echo 正在关闭以下端口的服务：
echo - 8761  (Eureka Server)
echo - 8080  (Gateway Service)
echo - 8086  (Auth Service)
echo - 8081  (Product Service)
echo - 8087  (Shop Service)
echo - 8082  (Order Service)
echo - 8083  (Contact Service)
echo - 8084  (Logistics Service)
echo - 8085  (Chat Service)
echo.

for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8761') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8080') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8086') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8081') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8087') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8082') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8084') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8083') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr 8085') do taskkill /F /PID %%a 2>nul

taskkill /F /IM java.exe 2>nul

echo.
echo All services stopped.
pause