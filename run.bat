@echo off
setlocal
echo ============================================
echo   OS Kernel Simulator
echo ============================================

set "PROJECT_DIR=%~dp0"

REM Check if compiled classes exist
if not exist "%PROJECT_DIR%out\com\ossimulator\Main.class" (
    echo Error: Project not compiled. Run 'compile.bat' first.
    pause
    exit /b 1
)

REM Create logs directory if needed
if not exist "%PROJECT_DIR%logs" mkdir "%PROJECT_DIR%logs"

REM Run the simulator with any arguments passed
java -cp "%PROJECT_DIR%out" com.ossimulator.Main %*

endlocal
