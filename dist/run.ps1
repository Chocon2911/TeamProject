# OS Kernel Simulator - PowerShell Runner
# Requires Java 17+

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  OS Kernel Simulator - CS4448" -ForegroundColor Cyan
Write-Host "  Requires Java 17 or higher" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Java is not installed!" -ForegroundColor Red
    Write-Host "Install from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

# Run JAR
java -jar os-simulator.jar

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Simulation Complete!" -ForegroundColor Green
Write-Host "  Log file: logs\simulation.log" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan

Read-Host "Press Enter to exit"
