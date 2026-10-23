@echo off
rem TrailCraft: get the latest changes from GitHub and start the game with the mod (Gradle runClient).
rem Local work in progress is never touched: with uncommitted changes the update is skipped.
setlocal
cd /d "%~dp0.."
call "%~dp0find-java.bat" || goto :fail

echo Updating from GitHub...
git diff --quiet && git diff --cached --quiet
if errorlevel 1 (
    echo There are uncommitted changes on this PC: update skipped, starting the local version.
) else (
    git pull --ff-only
    if errorlevel 1 echo Update failed ^(no network or the branches diverged^): starting the local version.
)

echo Starting the game...
call .\gradlew.bat runClient
if errorlevel 1 goto :fail
exit /b 0

:fail
echo.
echo Something went wrong, see the messages above.
pause
exit /b 1
