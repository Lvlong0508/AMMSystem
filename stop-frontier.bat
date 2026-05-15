@echo off
chcp 65001 >nul 2>&1

echo =========================================
echo AI-Shopping frontier stopping...
echo =========================================

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

taskkill /f /im node.exe >nul 2>&1

echo All frontier services stopped.
pause