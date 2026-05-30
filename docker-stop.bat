@echo off
cd /d %~dp0

echo Stopping volunteer system Docker services...
docker compose down

echo.
echo Stopped.
pause
