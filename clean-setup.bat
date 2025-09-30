@echo off
REM Clean Setup Script for Keycloak BioID Extension (Windows)
REM This script removes all containers, volumes, and provides a fresh start

echo 🧹 Cleaning up Keycloak BioID setup...

REM Stop all services
echo Stopping services...
docker compose down

REM Remove volumes (this will delete all data!)
echo Removing volumes (this will delete all Keycloak data)...
docker volume rm bioid-face-recognition-keycloak_postgres_data 2>nul || echo Volume already removed or doesn't exist
docker volume rm bioid-face-recognition-keycloak_prometheus_data 2>nul || echo Volume already removed or doesn't exist
docker volume rm bioid-face-recognition-keycloak_grafana_data 2>nul || echo Volume already removed or doesn't exist

REM Remove any dangling containers
echo Removing any dangling containers...
docker container prune -f

REM Optional: Remove the realm JSON to prevent automatic import
set /p choice="Do you want to disable automatic realm import? (y/N): "
if /i "%choice%"=="y" (
    if exist "docker\keycloak\bioid-demo-realm.json" (
        move "docker\keycloak\bioid-demo-realm.json" "docker\keycloak\bioid-demo-realm.json.backup"
        echo ✅ Realm JSON file backed up to prevent automatic import
        echo    You can restore it later with: move docker\keycloak\bioid-demo-realm.json.backup docker\keycloak\bioid-demo-realm.json
    ) else (
        echo ℹ️  Realm JSON file not found or already disabled
    )
)

echo.
echo ✅ Cleanup complete!
echo.
echo To start fresh:
echo 1. docker compose up -d
echo 2. Wait for services to start (check with: docker compose logs -f keycloak)
echo 3. Go to http://localhost:8080/admin and login with admin/admin123
echo 4. Follow the manual setup guide in MANUAL_SETUP.md
echo.
echo Or to re-enable automatic import:
echo 1. Restore the realm file: move docker\keycloak\bioid-demo-realm.json.backup docker\keycloak\bioid-demo-realm.json
echo 2. Update docker-compose.yml to add --import-realm to the command
echo 3. Add the volume mount back to docker-compose.yml

pause