@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title Bien Dich Multichat UDP Web

echo ========================================================
echo        BIEN DICH DU AN MULTICHAT UDP WEB JSP
echo ========================================================
echo.

if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong tim thay trinh bien dich javac. Kiem tra JAVA_HOME!
    pause
    exit /b 1
)

REM Giai phong file class neu Tomcat dang chay
netstat -ano | findstr :8088 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [THONG BAO] Dang tam dung may chu Tomcat tren cong 8088 de giai phong file class...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8088 ^| findstr LISTENING') do taskkill /f /pid %%a >nul 2>&1
    ping 127.0.0.1 -n 2 >nul
)

echo Dang bien dich ma nguon Java vao web\WEB-INF\classes...

if not exist "web\WEB-INF\classes" mkdir "web\WEB-INF\classes"
if exist "tomcat\work\Catalina" (
    rmdir /s /q "tomcat\work\Catalina" >nul 2>&1
)

javac -encoding UTF-8 -cp "tomcat\lib\*" -d web\WEB-INF\classes src\multichat_udp\*.java src\multichat_udp\servlet\*.java

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo [THANH CONG] Da bien dich xong tat ca cac lop Java!
    echo ========================================================
) else (
    echo.
    echo [LOI] Qua trinh bien dich that bai. Vui long kiem tra lai ma nguon.
)
echo.
pause
