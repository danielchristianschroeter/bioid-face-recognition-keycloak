#!/bin/bash

# Clean Setup Script for Keycloak BioID Extension
# This script removes all containers, volumes, and provides a fresh start

set -e

echo "🧹 Cleaning up Keycloak BioID setup..."

# Stop all services
echo "Stopping services..."
docker compose down

# Remove volumes (this will delete all data!)
echo "Removing volumes (this will delete all Keycloak data)..."
docker volume rm bioid-face-recognition-keycloak_postgres_data 2>/dev/null || echo "Volume already removed or doesn't exist"
docker volume rm bioid-face-recognition-keycloak_prometheus_data 2>/dev/null || echo "Volume already removed or doesn't exist"
docker volume rm bioid-face-recognition-keycloak_grafana_data 2>/dev/null || echo "Volume already removed or doesn't exist"

# Remove any dangling containers
echo "Removing any dangling containers..."
docker container prune -f

# Optional: Remove the realm JSON to prevent automatic import
read -p "Do you want to disable automatic realm import? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [ -f "docker/keycloak/bioid-demo-realm.json" ]; then
        mv docker/keycloak/bioid-demo-realm.json docker/keycloak/bioid-demo-realm.json.backup
        echo "✅ Realm JSON file backed up to prevent automatic import"
        echo "   You can restore it later with: mv docker/keycloak/bioid-demo-realm.json.backup docker/keycloak/bioid-demo-realm.json"
    else
        echo "ℹ️  Realm JSON file not found or already disabled"
    fi
fi

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "To start fresh:"
echo "1. docker compose up -d"
echo "2. Wait for services to start (check with: docker compose logs -f keycloak)"
echo "3. Go to http://localhost:8080/admin and login with admin/admin123"
echo "4. Follow the manual setup guide in MANUAL_SETUP.md"
echo ""
echo "Or to re-enable automatic import:"
echo "1. Restore the realm file: mv docker/keycloak/bioid-demo-realm.json.backup docker/keycloak/bioid-demo-realm.json"
echo "2. Update docker-compose.yml to add --import-realm to the command"
echo "3. Add the volume mount back to docker-compose.yml"