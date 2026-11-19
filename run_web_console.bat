@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title UDP Multichat Web - Console Log Mode

echo ========================================================
echo   UDP MULTICHAT WEB (CHE DO HIEN THI LOG TRUC TIEP)
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

echo Dang khoi dong may chu Web va lang nghe UDP...
echo Nhan Ctrl+C de dung may chu.
echo.

call "%~dp0tomcat\bin\catalina.bat" run
pause
