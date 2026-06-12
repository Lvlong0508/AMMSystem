@echo off
setlocal enabledelayedexpansion

set "NACOS_BIN=F:\nacos-server-3.2.2\nacos\bin"

echo =========================================
echo Nacos starting...
echo =========================================

:: 如果 JAVA_HOME 未设置，自动从 PATH 中检测
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME not set, auto-detecting from PATH...
    set "JAVA_HOME_FOUND="
    for /f "delims=" %%i in ('where java 2^>nul') do (
        if not defined JAVA_HOME_FOUND (
            rem 取 java.exe 所在目录的父目录，即为 JAVA_HOME
            for %%a in ("%%~dpi.") do set "JAVA_HOME_FOUND=%%~fa"
        )
    )
    if defined JAVA_HOME_FOUND (
        echo Detected JAVA_HOME: !JAVA_HOME_FOUND!
        set "JAVA_HOME=!JAVA_HOME_FOUND!"
    ) else (
        echo [ERROR] Cannot find java.exe in PATH.
        echo Please install JDK or set JAVA_HOME manually.
        pause
        goto :end
    )
)

:: 在新窗口内先设置 JAVA_HOME，再启动 Nacos
:: /k 让窗口保持打开，方便查看启动日志
start "Nacos Server" /D "%NACOS_BIN%" cmd /k "set ""JAVA_HOME=%JAVA_HOME%"" && call startup.cmd -m standalone"

echo.
echo Nacos startup command executed.
echo Input q to shutdown Nacos.
echo.

:wait_input
set /p "USER_INPUT=> "
if /I "%USER_INPUT%"=="q" goto stop_nacos
goto wait_input

:stop_nacos
call "%~dp0stop-nacos.bat"
goto :end

:end