@echo off
setlocal enabledelayedexpansion

@REM Einfach immer bauen...
call build.bat
if %ERRORLEVEL% neq 0 (
    echo Bob der Baumeister failed to build
    exit /b 1
)

echo normal
for /L %%i in (1,1,3) do (
    echo Iteration %%i
    java -XX:+UseG1GC -XX:+UseStringDeduplication -cp "bin;bin/de/codecoverage/utils/*" de.codecoverage.utils.TestDriver > output.log 2>&1
    findstr /i "Exception" output.log >nul
    if !errorlevel! equ 0 (
        echo Fehler im Iteration %%i.
        exit /b 1
    )
)

echo 33GB Heap
for /L %%i in (1,1,3) do (
    echo Iteration %%i
    java -Xmx33G -Xms33G -XX:+UseG1GC -XX:+UseStringDeduplication -cp "bin;bin/de/codecoverage/utils/*" de.codecoverage.utils.TestDriver > output.log 2>&1
    findstr /i "Exception" output.log >nul
    if !errorlevel! equ 0 (
        echo Fehler im Iteration %%i.
        exit /b 1
    )
)

exit /b 0
