@echo off
setlocal
cd /d "%~dp0"
title UDP Multichat CLI

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ==================================================
echo   Khoi chay UDP Multichat o che do dong lenh - CLI
echo ==================================================
java -Dfile.encoding=UTF-8 -jar dist\Multichat_UDP.jar cli %*
pause
