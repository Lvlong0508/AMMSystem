@echo off

set "NACOS_BIN=F:\nacos-server-3.2.2\nacos\bin"

echo =========================================
echo Nacos starting...
echo =========================================

start "Nacos Server" /D "%NACOS_BIN%" cmd /c "startup.cmd -m standalone"

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
