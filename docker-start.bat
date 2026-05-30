@echo off
cd /d %~dp0

echo Starting volunteer system with Docker...
echo.

docker compose up -d --build

if errorlevel 1 (
    echo.
    echo Failed to start. Please make sure Docker Desktop is installed and running.
    pause
    exit /b 1
)

echo.
echo Started successfully.
echo Open: http://localhost:28888/active-site.html
echo.
echo Default accounts:
echo Admin: admin / 123456
echo Organizer: organizer / 123456
echo Volunteer: volunteer / 123456
echo.
pause
