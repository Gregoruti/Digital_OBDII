@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

:: ============================================================================
:: AUTOEXEC.BAT - COMPILAÇÃO, UPLOAD GOOGLE DRIVE & NOTIFICAÇÃO POR E-MAIL
:: Projeto: Digital_OBDII
:: Destinatário: gregoruti@gmail.com
:: ============================================================================

cd /d "D:\Softwares\Digital_OBDII"

title Digital OBD-II - Compilacao e Distribuicao Automatica

echo ============================================================================
echo   DIGITAL OBD-II - COMPILAÇÃO E DISTRIBUIÇÃO AUTOMÁTICA
echo ============================================================================
echo.
echo  Projeto: Digital_OBDII
echo  Destinatário: gregoruti@gmail.com
echo  Destino: Google Drive (Pasta 'Digital_OBDII_APKs')
echo  Diretório: %CD%
echo.

if not "%~1"=="" (
    set "OPT=%~1"
    goto :PROCESSAR_OPCAO
)

echo  Selecione a ação desejada:
echo.
echo  [1] COMPILAR E ENVIAR APK (Recomendado)
echo      - Compila o APK Debug via Gradle (:app:assembleDebug)
echo      - Faz upload automático do APK (~55 MB) para o Google Drive
echo      - Envia o link de download formatado para gregoruti@gmail.com
echo.
echo  [2] APENAS ENVIAR APK EXISTENTE (Sem recompilar)
echo      - Pega o último APK gerado em app\build\outputs\apk\debug\
echo      - Faz upload para o Google Drive e envia o e-mail
echo.
echo  [3] COMPILAR APENAS (Sem enviar para o Drive)
echo      - Executa :app:assembleDebug localmente
echo.
echo  [0] Sair / Cancelar
echo.

choice /C 1230 /N /M "Digite sua escolha [1, 2, 3 ou 0] (Padrão: 1): "
if errorlevel 4 goto :CANCELAR
if errorlevel 3 set "OPT=3"
if errorlevel 2 set "OPT=2"
if errorlevel 1 set "OPT=1"

:PROCESSAR_OPCAO

if "%OPT%"=="0" goto :CANCELAR
if "%OPT%"=="2" goto :APENAS_ENVIAR
if "%OPT%"=="3" goto :APENAS_COMPILAR

:: ----------------------------------------------------------------------------
:: [OPÇÃO 1] COMPILAR E ENVIAR
:: ----------------------------------------------------------------------------
echo.
echo ============================================================================
echo [PASSO 1/2] Compilando APK do Digital_OBDII via Gradle...
echo ============================================================================
echo.

call gradlew.bat :app:assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================================================
    echo [ERRO] Falha na compilação do Gradle! O upload foi cancelado.
    echo Verifique os logs de erro acima antes de tentar novamente.
    echo ============================================================================
    goto :FIM
)

echo.
echo [OK] Compilação concluída com sucesso!
echo.

:APENAS_ENVIAR
echo ============================================================================
echo [PASSO 2/2] Carregando APK no Google Drive e Enviando E-mail...
echo ============================================================================
echo.

if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo [ERRO] O arquivo 'app-debug.apk' não foi encontrado em:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Por favor, execute a opção [1] para compilar o APK primeiro.
    goto :FIM
)

python scripts\upload_to_drive_and_email.py

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [AVISO] Houve um aviso ou pendência durante a execução do script.
    echo Verifique as mensagens acima (ex: credentials.json ou senha de app).
) else (
    echo.
    echo ============================================================================
    echo [SUCESSO COMPLETO] APK enviado para o Drive e e-mail disparado!
    echo Verifique sua caixa de entrada em gregoruti@gmail.com
    echo ============================================================================
)
goto :FIM

:: ----------------------------------------------------------------------------
:: [OPÇÃO 3] APENAS COMPILAR
:: ----------------------------------------------------------------------------
:APENAS_COMPILAR
echo.
echo ============================================================================
echo Compilando APK localmente (sem envio)...
echo ============================================================================
echo.

call gradlew.bat :app:assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] Compilação finalizada com sucesso!
    echo APK disponível em: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo [ERRO] Falha na compilação do Gradle.
)
goto :FIM

:CANCELAR
echo.
echo Operação cancelada pelo usuário.

:FIM
echo.
pause
