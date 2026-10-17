@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================================
echo   TechNest - Biên dịch và đóng gói ứng dụng (Java 17+)
echo ========================================================

where javac >nul 2>nul
if errorlevel 1 (
    echo [LỖI] Không tìm thấy 'javac'. Vui lòng cài đặt JDK 17 trở lên và cấu hình PATH.
    pause
    exit /b 1
)

if not exist "out" mkdir "out"

echo [*] Đang tìm kiếm mã nguồn Java trong thư mục src...
dir /s /b src\*.java > sources.txt

echo [*] Đang biên dịch mã nguồn Java (UTF-8)...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo [LỖI] Biên dịch thất bại!
    if exist sources.txt del sources.txt
    pause
    exit /b 1
)
if exist sources.txt del sources.txt

echo [*] Đang đóng gói TechNest.jar...
jar --create --file TechNest.jar --main-class com.thietbidientu.web.WebMain -C out .
if errorlevel 1 (
    echo [LỖI] Không thể tạo file TechNest.jar!
    pause
    exit /b 1
)

echo.
echo [THÀNH CÔNG] Đã tạo thành công TechNest.jar!
echo - Chạy giao diện Web:  run.bat (hoặc CHAY_GIAO_DIEN.cmd)
echo - Chạy bản Console:     run-cli.bat
echo ========================================================
pause
