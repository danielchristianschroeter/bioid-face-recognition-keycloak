@echo off
echo Building Keycloak BioID Extension...
echo ====================================

REM Clean and build the extension
echo Cleaning previous build...
mvn clean

if %errorlevel% neq 0 (
    echo Failed to clean project
    pause
    exit /b 1
)

echo Building extension...
mvn package -DskipTests

if %errorlevel% neq 0 (
    echo Build failed
    pause
    exit /b 1
)

echo.
echo ✅ Build completed successfully!
echo.
echo Extension JAR location:
dir deployment\target\keycloak-bioid-extension-*.jar

echo.
echo Next steps:
echo 1. Configure your .env file with BioID credentials
echo 2. Run: docker compose up -d
echo 3. Access Keycloak at http://localhost:8080
echo.
pause