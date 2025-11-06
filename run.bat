@echo off
REM JCrawler Launcher for Windows
REM This script runs the application using Maven's JavaFX plugin

echo Starting JCrawler...
echo.

mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to start JCrawler
    echo Make sure Maven is installed and in your PATH
    echo.
    pause
)
