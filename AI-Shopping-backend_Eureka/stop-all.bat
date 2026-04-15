@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ==========================================
echo     AI-Shopping 微服务关闭脚本
echo ==========================================
echo.

:: 定义服务列表（唯一配置区，后续加服务改这里即可）
set "SERVICE_LIST=chat-service order-service logistics-service contact-service product-service eureka-server"

echo [1/3] 正在关闭服务窗口...
:: 1. 尝试通过窗口标题关闭（辅助手段）
taskkill /F /FI "WINDOWTITLE eq Chat Service" 2>nul
taskkill /F /FI "WINDOWTITLE eq Order Service" 2>nul
taskkill /F /FI "WINDOWTITLE eq Logistics Service" 2>nul
taskkill /F /FI "WINDOWTITLE eq Contact Service" 2>nul
taskkill /F /FI "WINDOWTITLE eq Product Service" 2>nul
taskkill /F /FI "WINDOWTITLE eq Eureka Server" 2>nul

timeout /t 2 /nobreak >nul

echo [2/3] 正在强制清理 Java 进程...
:: 2. 通过命令行精准击杀（主要手段）
for %%s in (%SERVICE_LIST%) do (
    taskkill /F /IM "java.exe" /FI "COMMANDLINE contains %%s" 2>nul
)

timeout /t 2 /nobreak >nul

echo [3/3] 正在验证关闭结果...
:: 3. 验证与补刀（优化版：暂存 PID）
set "STILL_RUNNING=0"
set "RESIDUAL_INFO=" :: 用于存储 "服务名:PID" 的列表

:: 第一次验证 + 补刀
for %%s in (%SERVICE_LIST%) do (
    wmic process where "CommandLine like '%%%s%%' and Name='java.exe'" get ProcessId 2>nul | findstr /R "[0-9]" >nul
    if not errorlevel 1 (
        echo [警告] %%s 仍在运行，尝试补刀...
        taskkill /F /IM "java.exe" /FI "COMMANDLINE contains %%s" 2>nul
    )
)

timeout /t 2 /nobreak >nul

:: 二次验证（核心优化：这里不仅检查，还把 PID 存起来）
for %%s in (%SERVICE_LIST%) do (
    for /f "tokens=*" %%p in ('wmic process where "CommandLine like '%%%s%%' and Name='java.exe'" get ProcessId 2^>nul ^| findstr /R "[0-9]"') do (
        set "PID=%%p"
        :: 去除 wmic 输出可能带的多余空格/回车
        for /f "delims=" %%q in ("!PID!") do set "PID=%%q"
        
        if not "!PID!"=="" (
            echo [错误] %%s 未能关闭，PID: !PID!
            set "RESIDUAL_INFO=!RESIDUAL_INFO! [%%s:!PID!]"
            set /a STILL_RUNNING+=1
        )
    )
)

echo.
echo ==========================================
if !STILL_RUNNING! EQU 0 (
    echo     ✅ 所有服务已成功关闭！
) else (
    echo     ⚠️  有 !STILL_RUNNING! 个服务需手动处理
    echo.
    echo     残留进程：!RESIDUAL_INFO!
    echo.
    echo     请手动执行以下命令（或在任务管理器结束对应 PID）：
    for %%s in (%SERVICE_LIST%) do (
        :: 这里我们利用刚才暂存的逻辑思路，简化输出
        :: 实际上因为上面已经存了 RESIDUAL_INFO，这里可以直接提示通用格式
        echo     taskkill /PID ^<PID^> /F
    )
)
echo ==========================================
echo.
pause