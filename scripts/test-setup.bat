@echo off
setlocal enabledelayedexpansion

REM Test script for Keycloak BioID Extension Docker setup on Windows
REM This script verifies that the Docker Compose setup is working correctly

REM Change to project root directory
cd /d "%~dp0\.."

echo Testing Keycloak BioID Extension Docker Setup
echo ================================================
echo.

REM Function to print status messages
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "NC=[0m"

REM Check prerequisites
echo Checking prerequisites...

REM Check Docker
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Docker is installed
) else (
    echo %RED%[ERROR]%NC% Docker is not installed
    echo Please install Docker Desktop for Windows
    pause
    exit /b 1
)

REM Check Docker Compose
docker compose --version >nul 2>&1 || docker compose version >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Docker Compose is available
) else (
    echo %RED%[ERROR]%NC% Docker Compose is not available
    pause
    exit /b 1
)

REM Check if .env file exists
if exist ".env" (
    echo %GREEN%[OK]%NC% .env file exists
    
    REM Check if BWS credentials are set (basic check)
    findstr /C:"BWS_CLIENT_ID=your_" .env >nul 2>&1
    if !errorlevel! equ 0 (
        echo %YELLOW%[WARNING]%NC% .env file contains placeholder values - update with real BioID credentials
    ) else (
        echo %GREEN%[OK]%NC% BioID credentials appear to be configured
    )
) else (
    echo %YELLOW%[WARNING]%NC% .env file not found - copying from .env.example
    copy .env.example .env >nul
    echo %YELLOW%[WARNING]%NC% Please edit .env with your BioID credentials before continuing
)

REM Check if extension JAR exists
if exist "deployment\target\keycloak-bioid-extension-1.0.0-SNAPSHOT.jar" (
    echo %GREEN%[OK]%NC% Extension JAR exists
) else (
    echo %RED%[ERROR]%NC% Extension JAR not found - run 'mvn clean package' first
    pause
    exit /b 1
)

echo.
echo Starting Docker Compose services...

REM Start services
docker compose up -d
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Docker Compose services started
) else (
    echo %RED%[ERROR]%NC% Failed to start Docker Compose services
    pause
    exit /b 1
)

echo.
echo Waiting for services to be ready...

REM Wait for PostgreSQL
echo Waiting for PostgreSQL...
set /a counter=0
:wait_postgres
docker compose exec -T postgres pg_isready -U keycloak >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% PostgreSQL is ready
    goto postgres_ready
)
set /a counter+=1
if %counter% geq 30 (
    echo %RED%[ERROR]%NC% PostgreSQL failed to start within timeout
    goto postgres_ready
)
timeout /t 2 /nobreak >nul
goto wait_postgres
:postgres_ready

REM Wait for Keycloak
echo Waiting for Keycloak...
set /a counter=0
:wait_keycloak
curl -s http://localhost:8080/health/ready >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Keycloak is ready
    goto keycloak_ready
)
set /a counter+=1
if %counter% geq 60 (
    echo %RED%[ERROR]%NC% Keycloak failed to start within timeout
    goto keycloak_ready
)
timeout /t 3 /nobreak >nul
goto wait_keycloak
:keycloak_ready

echo.
echo Testing endpoints...

REM Test Keycloak health
curl -s http://localhost:8080/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Keycloak health endpoint responding
) else (
    echo %RED%[ERROR]%NC% Keycloak health endpoint not responding
)

REM Test Keycloak admin console
curl -s http://localhost:8080/admin/ >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Keycloak admin console accessible
) else (
    echo %RED%[ERROR]%NC% Keycloak admin console not accessible
)

REM Test metrics endpoint
curl -s http://localhost:9000/metrics >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% Metrics endpoint responding
) else (
    echo %RED%[ERROR]%NC% Metrics endpoint not responding
)

REM Check if extension is loaded
docker compose logs keycloak | findstr /C:"BioID" >nul 2>&1
if %errorlevel% equ 0 (
    echo %GREEN%[OK]%NC% BioID extension appears to be loaded
) else (
    echo %YELLOW%[WARNING]%NC% BioID extension may not be loaded - check logs
)

echo.
echo Service Status:
docker compose ps

echo.
echo Setup test completed!
echo.
echo Next steps:
echo 1. Access Keycloak Admin Console: http://localhost:8080/admin
echo 2. Login with: admin / admin123
echo 3. Create a realm and configure face authentication
echo 4. See SETUP.md for detailed configuration instructions
echo.
echo To view logs: docker compose logs -f keycloak
echo To stop services: docker compose down
echo.
pause