@echo off


set "BASE_DIR=%~dp0"

echo =========================================
echo AI-Shopping frontier starting...
echo =========================================

start "User Frontend" /D "%BASE_DIR%AI-Shopping-frontier\frontier-user" cmd /c "npm run dev"

timeout /t 2 /nobreak >nul

start "Seller Frontend" /D "%BASE_DIR%AI-Shopping-frontier\frontier-seller" cmd /c "npm run dev"

echo.
echo All frontier services started.
echo.
echo Press Q to stop all services, or press other key to exit (services will continue running)
echo.

:wait_loop
choice /C QX /N /T 1 /D X >nul
if errorlevel 2 goto wait_loop
if errorlevel 1 goto stop_services

:stop_services
call "%~dp0stop-frontier.bat"