@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================================
echo   TechNest - Website Quản Lý Bán Thiết Bị Điện Tử
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
    if not exist "TechNest.jar" (
        echo [LỖI] Không thể khởi động do chưa có TechNest.jar.
        pause
        exit /b 1
    )
)

echo.
echo [*] Đang khởi động máy chủ Web tại http://127.0.0.1:8080
echo [*] Mẹo: Bạn có thể mở trình duyệt và truy cập: http://127.0.0.1:8080
echo [*] Để dừng máy chủ, nhấn tổ hợp phím: Ctrl + C
echo ========================================================
echo.

:: Tự động mở trình duyệt sau 1 giây
start "" http://127.0.0.1:8080

java -Dfile.encoding=UTF-8 -jar TechNest.jar %*
pause
