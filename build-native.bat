@echo off
REM Build Native Windows Executable for JCrawler
REM This creates a standalone .exe with bundled JRE and JavaFX

echo ========================================
echo JCrawler Native Build Script
echo ========================================
echo.

REM Step 1: Build the application
echo [1/3] Building application with Maven...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)
echo Build successful!
echo.

REM Step 2: Create runtime image with jlink
echo [2/3] Creating custom runtime with jlink...
if exist target\runtime rmdir /s /q target\runtime

jlink --add-modules java.base,java.desktop,java.sql,java.naming,java.management,java.instrument,java.xml,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics,javafx.web ^
      --output target\runtime ^
      --strip-debug ^
      --no-header-files ^
      --no-man-pages ^
      --compress=2

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed - Make sure JavaFX jmods are in JAVA_HOME
    echo.
    echo Download JavaFX jmods from: https://gluonhq.com/products/javafx/
    echo Extract to: %%JAVA_HOME%%\jmods
    pause
    exit /b 1
)
echo Runtime created!
echo.

REM Step 3: Create native installer with jpackage
echo [3/3] Creating Windows executable...
if exist target\installer rmdir /s /q target\installer

jpackage --type app-image ^
         --input target ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --runtime-image target\runtime ^
         --dest target\installer ^
         --app-version 1.0.0 ^
         --vendor "JCrawler" ^
         --description "Lightweight Web Crawler Desktop Application"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! Native executable created
echo ========================================
echo.
echo Location: target\installer\JCrawler\JCrawler.exe
echo.
echo You can now:
echo 1. Run: target\installer\JCrawler\JCrawler.exe
echo 2. Or zip target\installer\JCrawler folder for distribution
echo.
echo The JCrawler folder is fully portable - no Java installation needed!
echo.
pause
