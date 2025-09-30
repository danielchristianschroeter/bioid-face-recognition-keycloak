@echo off
REM Keycloak BioID Extension Setup Verification Script for Windows

echo 🔍 Verifying Keycloak BioID Extension Setup...
echo ================================================

REM Check if Docker containers are running
echo 1. Checking Docker containers...
docker compose ps | findstr "keycloak-bioid.*Up" >nul
if %errorlevel% == 0 (
    echo ✅ Keycloak container is running
) else (
    echo ❌ Keycloak container is not running
    echo    Run: docker compose up -d
    exit /b 1
)

docker compose ps | findstr "keycloak-postgres.*Up" >nul
if %errorlevel% == 0 (
    echo ✅ PostgreSQL container is running
) else (
    echo ❌ PostgreSQL container is not running
    exit /b 1
)

REM Check Keycloak health
echo.
echo 2. Checking Keycloak health...
curl -s http://localhost:8080 >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ Keycloak is ready
) else (
    echo ❌ Keycloak is not ready
    echo    Check logs: docker compose logs keycloak
    exit /b 1
)

REM Check if bioid-demo realm exists
echo.
echo 3. Checking bioid-demo realm...
curl -s http://localhost:8080/realms/bioid-demo/account >nul 2>&1
if %errorlevel% == 0 (
    echo ✅ bioid-demo realm is accessible
) else (
    echo ❌ bioid-demo realm is not accessible
    echo    Check realm import logs: docker compose logs keycloak ^| findstr /i import
    exit /b 1
)

REM Check if extension is loaded
echo.
echo 4. Checking BioID extension...
docker compose logs keycloak | findstr /i "bioid" >nul
if %errorlevel% == 0 (
    echo ✅ BioID extension logs found
) else (
    echo ⚠️  No BioID extension logs found (this might be normal)
)

REM Provide next steps
echo.
echo 5. Testing demo realm access...
echo    Demo user login URL: http://localhost:8080/realms/bioid-demo/account
echo    Demo user credentials: demo / demo123
echo    Admin console: http://localhost:8080/admin/master/console/#/bioid-demo

echo.
echo 🎉 Setup verification complete!
echo.
echo Next steps:
echo 1. Open: http://localhost:8080/realms/bioid-demo/account
echo 2. Login with: demo / demo123
echo 3. Complete face enrollment when prompted
echo.
echo If you encounter issues:
echo - Check logs: docker compose logs -f keycloak
echo - Verify .env file has BWS_CLIENT_ID and BWS_KEY set
echo - Ensure you're using the bioid-demo realm URLs, not master realm