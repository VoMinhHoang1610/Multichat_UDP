@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title Dung UDP Multichat Web - Tomcat

echo ========================================================
echo        DUNG MAY CHU UDP MULTICHAT WEB TOMCAT
echo ========================================================
echo.

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

set "CATALINA_HOME=%~dp0tomcat"
set "CATALINA_BASE=%~dp0tomcat"

echo Dang gui lenh dung may chu Tomcat...
call "%~dp0tomcat\bin\shutdown.bat"

echo.
echo ========================================================
echo [OK] May chu da duoc gui tin hieu dung an toan.
echo ========================================================
echo.
ping 127.0.0.1 -n 3 >nul
