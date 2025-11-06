@echo off
REM Get the directory where this script is located
cd /d "%~dp0"

echo Running JCrawler with verbose output...
echo Current directory: %CD%
echo.

echo Starting application...
echo.

REM Check if jvm.dll exists
if exist target\installer\JCrawler\runtime\bin\server\jvm.dll (
    echo Found jvm.dll in server directory
) else (
    echo ERROR: jvm.dll not found!
    pause
    exit /b 1
)

REM Run with the launcher directly but capture output
cd target\installer\JCrawler
set JAVA_HOME=%CD%\runtime
set PATH=%CD%\runtime\bin;%PATH%

echo.
echo Running with verbose flags...
runtime\bin\server\jvm.dll
if %ERRORLEVEL% EQU 0 (
    echo JVM DLL found
)

REM Try to run the JAR directly with java launcher if it exists
if exist runtime\bin\javaw.exe (
    runtime\bin\javaw.exe -Dprism.verbose=true -Djavafx.verbose=true -jar app\jcrawler-1.0.0.jar
) else if exist runtime\bin\java.exe (
    runtime\bin\java.exe -Dprism.verbose=true -Djavafx.verbose=true -jar app\jcrawler-1.0.0.jar
) else (
    echo.
    echo ERROR: No java.exe or javaw.exe found in runtime\bin
    echo This is a jpackage issue - the JRE is incomplete
)

cd ..\..\..

echo.
echo Exit code: %ERRORLEVEL%
pause
