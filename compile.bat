@echo off
setlocal enabledelayedexpansion
echo ============================================
echo   OS Kernel Simulator - Compiling...
echo ============================================

REM Create output directory
if not exist "out" mkdir out

REM Get current directory
set "PROJECT_DIR=%~dp0"

REM Compile all Java files
echo Compiling Java files...
cd /d "%PROJECT_DIR%"

REM Build list of Java files with proper quoting
set "SOURCES="
for /r "%PROJECT_DIR%src\main\java" %%f in (*.java) do (
    set "SOURCES=!SOURCES! "%%f""
)

REM Compile
javac -d "%PROJECT_DIR%out" -sourcepath "%PROJECT_DIR%src\main\java" %SOURCES%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo   Compilation successful!
    echo   Run 'run.bat' to start the GUI
    echo   Run 'run.bat --console' for console mode
    echo ============================================
) else (
    echo.
    echo Compilation failed!
)

endlocal
pause
