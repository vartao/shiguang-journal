@echo off
title Deploy Qinglv Note

set SERVER_IP=103.149.93.129
set SERVER_USER=root
set REMOTE_DIR=/opt/qinglv

echo ============================================
echo   Qinglv Note - One Click Deploy
echo   Target: %SERVER_IP%
echo ============================================
echo.
echo   You will need to enter server password TWICE.
echo   First build takes about 5-10 minutes.
echo.

cd /d "%~dp0"

echo [1/3] Packing project files...
tar -czf qinglv-deploy.tar.gz --exclude="node_modules" --exclude="target" --exclude=".git" --exclude="qinglv-deploy.tar.gz" backend frontend sql deploy docker-compose.yml .env.example
if %errorlevel% neq 0 (
    echo [ERROR] Failed to pack files.
    pause
    exit /b 1
)
echo       Done.
echo.

echo [2/3] Uploading to server (enter password)...
scp qinglv-deploy.tar.gz %SERVER_USER%@%SERVER_IP%:/tmp/
if %errorlevel% neq 0 (
    echo [ERROR] Upload failed. Check network and password.
    del /f qinglv-deploy.tar.gz 2>nul
    pause
    exit /b 1
)
echo       Upload complete.
echo.

echo [3/3] Deploying on server (enter password again)...
ssh %SERVER_USER%@%SERVER_IP% "mkdir -p %REMOTE_DIR% && tar -xzf /tmp/qinglv-deploy.tar.gz -C %REMOTE_DIR% && rm -f /tmp/qinglv-deploy.tar.gz && cd %REMOTE_DIR% && cp .env.example .env && sed -i 's/your-domain.com/%SERVER_IP%/g' deploy/nginx.conf && docker compose up -d --build"
if %errorlevel% neq 0 (
    echo [ERROR] Remote deploy failed. Check server Docker environment.
    del /f qinglv-deploy.tar.gz 2>nul
    pause
    exit /b 1
)

del /f qinglv-deploy.tar.gz 2>nul

echo.
echo ============================================
echo   SUCCESS!
echo.
echo   Open browser: http://%SERVER_IP%
echo.
echo   Register two accounts and start
echo   recording your love story!
echo ============================================
echo.
pause
