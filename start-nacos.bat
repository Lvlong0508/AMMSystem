@echo off
setlocal enabledelayedexpansion

set "NACOS_BIN=F:\nacos-server-3.2.2\nacos\bin"
set "JAVA_HOME=F:\JDK\jdk 17.0.17"

echo =========================================
echo Nacos starting...
echo =========================================

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