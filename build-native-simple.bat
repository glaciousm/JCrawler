@echo off
REM Simple Native Build - Creates standalone Windows executable
REM Requires: JDK 17+ with JavaFX installed

echo Building JCrawler native executable...
echo.

REM Build JAR
echo [1/2] Building JAR...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

REM Create native executable
echo.
echo [2/2] Creating native executable...
jpackage --type app-image ^
         --input target ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --dest target\installer ^
         --app-version 1.0.0

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: jpackage failed. Make sure you have JDK 17+ installed.
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Executable: target\installer\JCrawler\JCrawler.exe
echo.
echo Run it by double-clicking or: target\installer\JCrawler\JCrawler.exe
echo.
pause
