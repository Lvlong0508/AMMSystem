@echo off

set "NACOS_BIN=F:\nacos-server-3.2.2\nacos\bin"

echo =========================================
echo Nacos stopping...
echo =========================================

cd /d "%NACOS_BIN%"
call shutdown.cmd
