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

REM Copy JavaFX native DLLs from Maven repository
echo Copying JavaFX native libraries...
if not exist "%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1-win.jar" (
    echo ERROR: JavaFX native libraries not found in Maven repository
    echo Please run: mvn dependency:copy-dependencies first
    pause
    exit /b 1
)

REM Extract JavaFX DLLs from the native JARs
mkdir target\javafx-natives
cd target\javafx-natives
"%JAVA_HOME%\bin\jar.exe" xf "%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1-win.jar"
"%JAVA_HOME%\bin\jar.exe" xf "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1-win.jar"
"%JAVA_HOME%\bin\jar.exe" xf "%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21.0.1\javafx-fxml-21.0.1-win.jar"
"%JAVA_HOME%\bin\jar.exe" xf "%USERPROFILE%\.m2\repository\org\openjfx\javafx-web\21.0.1\javafx-web-21.0.1-win.jar"
"%JAVA_HOME%\bin\jar.exe" xf "%USERPROFILE%\.m2\repository\org\openjfx\javafx-media\21.0.1\javafx-media-21.0.1-win.jar"
cd ..\..

REM Copy all DLLs to jpackage input
echo Copying extracted DLLs...
if exist target\javafx-natives\*.dll copy target\javafx-natives\*.dll target\jpackage-input\
if exist target\javafx-natives\bin\*.dll copy target\javafx-natives\bin\*.dll target\jpackage-input\

REM Create native executable
echo.
echo [4/4] Creating native executable...
if exist target\installer\JCrawler rmdir /s /q target\installer\JCrawler
jpackage --type app-image ^
         --input target\jpackage-input ^
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

REM Copy JavaFX DLLs to the executable's bin directory
echo.
echo Copying JavaFX DLLs to runtime...
if exist target\javafx-natives\*.dll copy target\javafx-natives\*.dll target\installer\JCrawler\runtime\bin\
if exist target\javafx-natives\bin\*.dll copy target\javafx-natives\bin\*.dll target\installer\JCrawler\runtime\bin\

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
