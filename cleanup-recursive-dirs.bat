@echo off
REM Cleanup script for recursive directory structures
REM This deletes nested directories from the inside out

echo Cleaning up recursive directory structure...
echo.

REM Try using robocopy method (most reliable for deep paths)
if exist target\installer (
    echo Creating empty directory for robocopy...
    if not exist empty_temp mkdir empty_temp

    echo Using robocopy to mirror empty directory (this deletes content)...
    robocopy empty_temp target\installer /MIR /R:0 /W:0 > nul 2>&1

    echo Removing directories...
    rmdir /s /q target\installer 2>nul
    rmdir /s /q empty_temp 2>nul
)

REM Also clean jpackage-input
if exist target\jpackage-input (
    rmdir /s /q target\jpackage-input 2>nul
)

REM Clean old JARs
if exist target\*.jar (
    del /q target\*.jar 2>nul
)

echo.
echo Cleanup complete!
echo You can now run build-native-simple.bat
echo.
pause
