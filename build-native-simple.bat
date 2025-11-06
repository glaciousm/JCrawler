@echo off
REM Simple Native Build - Creates standalone Windows executable
REM Requires: JDK 17+ with JavaFX installed

echo Building JCrawler native executable...
echo.

REM Clean target directory (avoid Maven clean StackOverflowError on Windows)
echo [1/4] Cleaning target directory...
if exist target\installer (
    echo Removing old installer directory...
    rmdir /s /q target\installer 2>nul
)
if exist target\jpackage-input (
    echo Removing old jpackage-input directory...
    rmdir /s /q target\jpackage-input 2>nul
)
if exist target\*.jar (
    echo Removing old JAR files...
    del /q target\*.jar 2>nul
)

REM Build JAR (without clean to avoid path length issues)
echo.
echo [2/4] Building JAR...
call mvn package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

REM Prepare for jpackage (use shaded JAR to avoid path length issues)
echo.
echo [3/4] Preparing jpackage input...
if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy target\jcrawler-1.0.0.jar target\jpackage-input\

REM Download and prepare JavaFX jmods
echo Downloading JavaFX jmods package...
set JAVAFX_VERSION=21.0.1
set JAVAFX_JMODS_DIR=target\javafx-jmods
set JAVAFX_JMODS_ZIP=target\openjfx-%JAVAFX_VERSION%_windows-x64_bin-jmods.zip

REM Download JavaFX jmods from Gluon if not already present
if not exist "%JAVAFX_JMODS_ZIP%" (
    echo   Downloading from Gluon HQ...
    curl -L -o "%JAVAFX_JMODS_ZIP%" "https://download2.gluonhq.com/openjfx/%JAVAFX_VERSION%/openjfx-%JAVAFX_VERSION%_windows-x64_bin-jmods.zip"
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to download JavaFX jmods
        pause
        exit /b 1
    )
) else (
    echo   Using cached JavaFX jmods package
)

REM Extract jmods (always extract to ensure they're valid)
echo   Extracting jmods...
if exist %JAVAFX_JMODS_DIR% rmdir /s /q %JAVAFX_JMODS_DIR%
if exist target\javafx-temp rmdir /s /q target\javafx-temp
powershell -Command "Expand-Archive -Path '%JAVAFX_JMODS_ZIP%' -DestinationPath 'target\javafx-temp' -Force"
if not exist %JAVAFX_JMODS_DIR% mkdir %JAVAFX_JMODS_DIR%
xcopy /Y target\javafx-temp\javafx-jmods-%JAVAFX_VERSION%\*.jmod %JAVAFX_JMODS_DIR%\
rmdir /s /q target\javafx-temp

echo JavaFX jmods ready!

REM Create native executable with JavaFX modules
echo.
echo [4/4] Creating native executable with JavaFX runtime...
if exist target\installer\JCrawler rmdir /s /q target\installer\JCrawler
jpackage --type app-image ^
         --input target\jpackage-input ^
         --name JCrawler ^
         --main-jar jcrawler-1.0.0.jar ^
         --main-class com.jcrawler.JCrawlerApplication ^
         --dest target\installer ^
         --app-version 1.0.0 ^
         --module-path %JAVAFX_JMODS_DIR% ^
         --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.web,javafx.media,java.naming,java.sql,java.management

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
