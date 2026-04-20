@echo off

set "BASE_DIR=%~dp0"

echo Starting services...

start "eureka-server" /D "%BASE_DIR%eureka-server" cmd /c "mvn spring-boot:run"
timeout /t 3 /nobreak >nul

start "product-service" /D "%BASE_DIR%product-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "contact-service" /D "%BASE_DIR%contact-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "logistics-service" /D "%BASE_DIR%logistics-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "order-service" /D "%BASE_DIR%order-service" cmd /c "mvn spring-boot:run"
timeout /t 1 /nobreak >nul
start "chat-service" /D "%BASE_DIR%chat-service" cmd /c "mvn spring-boot:run"

echo.
echo Services started. Press Q to stop, or any key to exit.
echo.

:wait_loop
choice /C QX /N /T 1 /D X >nul
if errorlevel 2 goto wait_loop
if errorlevel 1 goto stop_services

:stop_services
call "%~dp0stop-all.bat"