@echo off
REM Get the directory where this script is located
cd /d "%~dp0"

echo Running JCrawler with verbose output...
echo Current directory: %CD%
echo.

echo Starting application...
target\installer\JCrawler\bin\java.exe ^
  -Dprism.verbose=true ^
  -Djavafx.verbose=true ^
  -jar target\installer\JCrawler\app\jcrawler-1.0.0.jar

echo.
echo Exit code: %ERRORLEVEL%
pause
