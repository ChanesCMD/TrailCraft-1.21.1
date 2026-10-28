@echo off
rem TrailCraft: get the latest changes from GitHub and start the game with the mod (Gradle runClient).
rem Local work in progress is never touched: with uncommitted changes the update is skipped.
rem How the start went is passed to Gradle in TRAILCRAFT_START_STATUS: build.gradle shows it at the bottom of the
rem console next to "> :runClient" and "> IDLE" while the game runs.
setlocal
cd /d "%~dp0.."
call "%~dp0find-java.bat" || goto :fail

echo Updating from GitHub...
set "TRAILCRAFT_START_STATUS="
set "BEHIND="
set "AHEAD="

git --version >nul 2>&1
if errorlevel 1 (
    set "TRAILCRAFT_START_STATUS=Git not found: started the local version"
    goto :start
)

git diff --quiet && git diff --cached --quiet
if errorlevel 1 (
    set "TRAILCRAFT_START_STATUS=Uncommitted changes on this PC: update skipped, started the local version"
    goto :start
)

rem no password prompt: without access fetch fails at once instead of waiting
set "GIT_TERMINAL_PROMPT=0"
git fetch --quiet
if errorlevel 1 (
    set "TRAILCRAFT_START_STATUS=No access to GitHub: started the local version"
    goto :start
)

for /f %%n in ('git rev-list --count HEAD..@{u} 2^>nul') do set "BEHIND=%%n"
for /f %%n in ('git rev-list --count @{u}..HEAD 2^>nul') do set "AHEAD=%%n"
if not defined BEHIND (
    set "TRAILCRAFT_START_STATUS=This branch is not linked to GitHub: started the local version"
    goto :start
)

if not "%AHEAD%"=="0" if not "%BEHIND%"=="0" (
    set "TRAILCRAFT_START_STATUS=Diverged from GitHub (%AHEAD% local, %BEHIND% on GitHub): started the local version"
    goto :start
)
if not "%AHEAD%"=="0" (
    set "TRAILCRAFT_START_STATUS=Up to date, but %AHEAD% local commits are not pushed to GitHub"
    goto :start
)
if "%BEHIND%"=="0" (
    set "TRAILCRAFT_START_STATUS=Already up to date with GitHub"
    goto :start
)

git merge --ff-only --quiet @{u}
if errorlevel 1 (
    set "TRAILCRAFT_START_STATUS=Update from GitHub failed: started the local version"
) else (
    set "TRAILCRAFT_START_STATUS=Updated from GitHub: %BEHIND% new commits"
)

:start
rem branch and commit the game is started from
for /f %%h in ('git rev-parse --abbrev-ref HEAD 2^>nul') do set "BRANCH=%%h"
for /f %%h in ('git rev-parse --short HEAD 2^>nul') do set "COMMIT=%%h"
if defined COMMIT set "TRAILCRAFT_START_STATUS=%TRAILCRAFT_START_STATUS% [%BRANCH% %COMMIT%]"
echo %TRAILCRAFT_START_STATUS%

rem A second copy of the game in the same run folder would crash: close the one started earlier first.
if exist "run\trailcraft-game.pid" powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-game.ps1" -PidFile "%CD%\run\trailcraft-game.pid"

echo Starting the game...
call .\gradlew.bat runClient
if errorlevel 1 goto :fail
exit /b 0

:fail
echo.
echo Something went wrong, see the messages above.
pause
exit /b 1
