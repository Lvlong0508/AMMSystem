@echo off
setlocal

set "BASE_DIR=%~dp0"

echo =========================================
echo AI-Shopping 前端启动脚本 (Windows Terminal)
echo =========================================
echo.
echo 正在启动前端服务...
echo.

start wt --title "用户端前端 (User)" --windowNumber 0 cmd /k "cd /d "%BASE_DIR%AI-Shopping-frontier\frontier-user" && npm run dev"
timeout /t 2 /nobreak >nul

start wt --title "商家端前端 (Seller)" cmd /k "cd /d "%BASE_DIR%AI-Shopping-frontier\frontier-seller" && npm run dev"

echo =========================================
echo 前端服务启动中：
echo - 用户端前端 (User)
echo - 商家端前端 (Seller)
echo =========================================
echo.
echo 请访问各标签页显示的地址查看前端页面
echo.
pause
endlocal