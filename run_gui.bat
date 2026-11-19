@echo off
setlocal
cd /d "%~dp0"
title UDP Multichat GUI

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Dang khoi chay UDP Multichat GUI...
start "" javaw -jar dist\Multichat_UDP.jar
