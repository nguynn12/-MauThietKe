$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  TechNest - Bien dich va Dong goi ung dung (Java 17+)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Error "Khong tim thay javac. Vui long cai dat JDK 17+ va them vao PATH."
    exit 1
}

New-Item -ItemType Directory -Force out | Out-Null
$sourceFiles = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }

Write-Host "[*] Dang bien dich cac tap tin Java..." -ForegroundColor Yellow
& javac -encoding UTF-8 -d out $sourceFiles
if ($LASTEXITCODE -ne 0) {
    Write-Error "Bien dich that bai. Yeu cau JDK 17 tro len."
    exit $LASTEXITCODE
}

Write-Host "[*] Dang dong goi TechNest.jar..." -ForegroundColor Yellow
& jar --create --file TechNest.jar --main-class com.thietbidientu.web.WebMain -C out .
if ($LASTEXITCODE -ne 0) {
    Write-Error "Khong the tao file TechNest.jar."
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "[THANH CONG] Da tao thanh cong TechNest.jar!" -ForegroundColor Green
Write-Host "- Chay Web GUI:  .\run.bat hoac java -jar TechNest.jar"
Write-Host "- Chay Console:  .\run-cli.bat hoac java -cp TechNest.jar com.thietbidientu.Main"
Write-Host "========================================================" -ForegroundColor Cyan
