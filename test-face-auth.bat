@echo off
REM BioID Face Authentication Test Script (Windows)
REM This script helps verify that your setup is working correctly

echo 🧪 BioID Face Authentication Test Script
echo ========================================
echo.

REM Check if Keycloak is running
echo 1. Checking Keycloak availability...
curl -s -f http://localhost:8080/health/ready >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Keycloak is running and ready
) else (
    echo ❌ Keycloak is not running or not ready
    echo    Run: docker compose up -d
    exit /b 1
)

REM Check if bioid-demo realm exists
echo.
echo 2. Checking if bioid-demo realm exists...
curl -s -f http://localhost:8080/realms/bioid-demo/.well-known/openid_configuration >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ bioid-demo realm is accessible
) else (
    echo ❌ bioid-demo realm not found
    echo    You need to create the realm manually:
    echo    1. Go to http://localhost:8080/admin
    echo    2. Login with admin/admin123
    echo    3. Create Realm → Browse → Select docker/keycloak/bioid-demo-realm.json
    echo    Or follow the manual setup guide in MANUAL_SETUP.md
    exit /b 1
)

REM Check if BioID extension is loaded
echo.
echo 3. Checking BioID extension...
docker compose logs keycloak 2>nul | findstr /i "BioID.*initialized" >nul
if %errorlevel% equ 0 (
    echo ✅ BioID extension is loaded and initialized
) else (
    echo ⚠️  BioID extension may not be properly initialized
    echo    Check logs: docker compose logs keycloak | findstr /i bioid
)

REM Check if test app files exist
echo.
echo 4. Checking test application files...
if exist "test-app.html" (
    echo ✅ test-app.html found
) else (
    echo ❌ test-app.html not found
    exit /b 1
)

if exist "serve-test-app.py" (
    echo ✅ serve-test-app.py found
) else (
    echo ❌ serve-test-app.py not found
    exit /b 1
)

REM Check Python availability
echo.
echo 5. Checking Python for test server...
python --version >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Python is available
    set PYTHON_CMD=python
) else (
    python3 --version >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Python 3 is available
        set PYTHON_CMD=python3
    ) else (
        echo ⚠️  Python not found - you can still open test-app.html directly in your browser
        set PYTHON_CMD=
    )
)

echo.
echo 🎉 Setup verification complete!
echo.
echo Next steps:
echo 1. Start the test server:
if defined PYTHON_CMD (
    echo    %PYTHON_CMD% serve-test-app.py
) else (
    echo    Open test-app.html directly in your browser
)
echo.
echo 2. Open http://localhost:3000/test-app.html (if using test server)
echo.
echo 3. Click 'Login with Face Authentication' and test the flow
echo.
echo 4. For account management, go to: http://localhost:8080/realms/bioid-demo/account
echo.
echo Troubleshooting:
echo - If you see 'Invalid redirect URI', make sure the client allows http://localhost:3000/*
echo - If account management doesn't work, try the manual setup in MANUAL_SETUP.md
echo - Check logs with: docker compose logs keycloak | findstr /i bioid

pause