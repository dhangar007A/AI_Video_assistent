@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
cd /d "%~dp0"

REM Load environment variables from ..\server\.env if present locally
if exist "..\server\.env" (
    for /f "usebackq tokens=1* delims==" %%A in ("..\server\.env") do (
        if not "%%A"=="" if not "%%B"=="" set "%%A=%%B"
    )
)

call .\mvnw.cmd spring-boot:run
pause
