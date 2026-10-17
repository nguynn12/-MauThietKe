@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================================
echo   TechNest - Chạy Kịch Bản Minh Họa Mẫu Thiết Kế (CLI)
echo ========================================================

where java >nul 2>nul
if errorlevel 1 (
    echo [LỖI] Chưa tìm thấy Java. Vui lòng cài đặt Java 17 trở lên và thêm vào PATH.
    pause
    exit /b 1
)

if not exist "TechNest.jar" (
    echo [*] Chưa tìm thấy TechNest.jar. Đang tự động biên dịch...
    call build.bat
)

java -Dfile.encoding=UTF-8 -cp TechNest.jar com.thietbidientu.Main
echo.
echo ========================================================
pause
