@echo off
echo Running JCrawler with verbose output...
echo.

cd target\installer\JCrawler

echo Starting application...
bin\java.exe ^
  -Dprism.verbose=true ^
  -Djavafx.verbose=true ^
  -jar app\jcrawler-1.0.0.jar

echo.
echo Exit code: %ERRORLEVEL%
pause
