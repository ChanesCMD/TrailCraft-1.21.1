@echo off
rem TrailCraft: build the mod jar from the code on this PC (build\libs\trailcraft-*.jar).
setlocal
cd /d "%~dp0.."
call "%~dp0find-java.bat" || goto :done
call .\gradlew.bat build
set "RESULT=%ERRORLEVEL%"
echo.
if not "%RESULT%"=="0" (
    echo BUILD FAILED
) else (
    echo Done, the jar is in build\libs
)
:done
pause
