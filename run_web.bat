@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
title Khoi Chay UDP Multichat Web - JSP

echo ========================================================
echo        HE THONG UDP MULTICHAT ENTERPRISE - JSP
echo ========================================================
echo.

REM 1. Kiem tra va thiet lap JAVA_HOME
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-25" set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong tim thay trinh bien dich JDK javac.
    echo Vui long kiem tra lai bien moi truong JAVA_HOME.
    echo Duong dan hien tai: %JAVA_HOME%
    echo.
    pause
    exit /b 1
)

REM 2. Giai phong may chu cu neu dang chay de cap nhat ma nguon sach
netstat -ano | findstr :8088 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [THONG BAO] Dang dong may chu Tomcat dang chay tren cong 8088 de cap nhat ban moi...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8088 ^| findstr LISTENING') do taskkill /f /pid %%a >nul 2>&1
    ping 127.0.0.1 -n 2 >nul
)

REM 3. Bien dich ma nguon Java moi nhat
echo [1/3] Dang bien dich ma nguon Java vao web\WEB-INF\classes...
if not exist "web\WEB-INF\classes" mkdir "web\WEB-INF\classes"
javac -encoding UTF-8 -cp "tomcat\lib\*" -d web\WEB-INF\classes src\multichat_udp\*.java src\multichat_udp\servlet\*.java
if %ERRORLEVEL% neq 0 (
    echo [LOI] Bien dich Java that bai. Vui long kiem tra lai ma nguon.
    echo.
    pause
    exit /b 1
)
echo [OK] Ma nguon Java da san sang va bien dich thanh cong!

REM 4. Khoi dong may chu Web Tomcat
set "CATALINA_HOME=%~dp0tomcat"
set "CATALINA_BASE=%~dp0tomcat"
echo [2/3] Dang khoi dong may chu Web Tomcat...
call "%~dp0tomcat\bin\startup.bat"

REM 5. Mo trinh duyet
echo [3/3] Dang mo trinh duyet tai http://localhost:8088/ ...
ping 127.0.0.1 -n 4 >nul
start http://localhost:8088/

echo.
echo ========================================================
echo [HOAN TAT] He thong Multichat UDP Web da san sang!
echo URL truy cap: http://localhost:8088/
echo De dung may chu, ban chay file: stop_web.bat
echo ========================================================
echo.
pause
