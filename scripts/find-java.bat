@echo off
rem Sets JAVA_HOME to a JDK 21: Gradle 9.2.1 does not run on newer Java (for example 26).
rem Called from play.bat and build.bat; exits with 1 if no JDK 21 is found.

if defined JAVA_HOME if exist "%JAVA_HOME%\release" (
    findstr /r /c:"JAVA_VERSION=.21" "%JAVA_HOME%\release" >nul && exit /b 0
)
call :search && exit /b 0

echo.
echo JDK 21 not found. TrailCraft needs Java 21 to build and run.
where winget >nul 2>nul || goto :manual
choice /m "Install Eclipse Temurin JDK 21 now with winget"
if errorlevel 2 goto :manual
winget install --id EclipseAdoptium.Temurin.21.JDK -e
call :search && exit /b 0

:manual
echo Install JDK 21 from https://adoptium.net/temurin/releases/?version=21 and run this again.
exit /b 1

:search
for /d %%D in ("%USERPROFILE%\.jdks\jdk-21*" "%ProgramFiles%\Eclipse Adoptium\jdk-21*" "%ProgramFiles%\Java\jdk-21*" "%ProgramFiles%\Microsoft\jdk-21*") do (
    if exist "%%~D\bin\java.exe" (
        set "JAVA_HOME=%%~D"
        exit /b 0
    )
)
exit /b 1
