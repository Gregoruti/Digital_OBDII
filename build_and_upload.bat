@echo off
title Digital OBD-II - Compilar e Enviar para Google Drive
echo ========================================================
echo   COMPILANDO DIGITAL_OBDII E DISTRIBUINDO VIA DRIVE
echo ========================================================
cd /d "%~dp0"

echo [1/2] Compilando APK Debug via Gradle...
call gradlew.bat :app:assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha na compilacao do Gradle! O upload foi cancelado.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Executando Upload para o Google Drive e Envio de E-mail...
python scripts\upload_to_drive_and_email.py

echo.
echo Concluido!
pause
