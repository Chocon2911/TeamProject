@echo off
title OS Kernel Simulator
echo.
echo ==========================================
echo   OS Kernel Simulator - CS4448
echo   Requires Java 17 or higher
echo ==========================================
echo.

:: Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17+ from: https://adoptium.net/
    pause
    exit /b 1
)

:: Run the JAR
java -jar os-simulator.jar

echo.
echo ==========================================
echo   Simulation Complete!
echo   Log file: logs\simulation.log
echo ==========================================
pause
