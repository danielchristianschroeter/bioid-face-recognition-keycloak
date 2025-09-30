# PowerShell script to build Keycloak BioID Extension

Write-Host "Building Keycloak BioID Extension..." -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan

# Clean and build the extension
Write-Host "Cleaning previous build..." -ForegroundColor Yellow
$cleanResult = & mvn clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to clean project" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Building extension..." -ForegroundColor Yellow
$buildResult = & mvn package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "✅ Build completed successfully!" -ForegroundColor Green
Write-Host ""

Write-Host "Extension JAR location:" -ForegroundColor Cyan
Get-ChildItem -Path "deployment\target\keycloak-bioid-extension-*.jar" | ForEach-Object { 
    Write-Host "  $($_.FullName)" -ForegroundColor White
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Configure your .env file with BioID credentials" -ForegroundColor White
Write-Host "2. Run: docker compose up -d" -ForegroundColor White
Write-Host "3. Access Keycloak at http://localhost:8080" -ForegroundColor White
Write-Host ""

Read-Host "Press Enter to exit"