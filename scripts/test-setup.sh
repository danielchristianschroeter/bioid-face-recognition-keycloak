#!/bin/bash

# Test script for Keycloak BioID Extension Docker setup
# This script verifies that the Docker Compose setup is working correctly

set -e

echo "🧪 Testing Keycloak BioID Extension Docker Setup"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "ℹ️  $1"
}

# Check prerequisites
echo "Checking prerequisites..."

# Check Docker
if command -v docker &> /dev/null; then
    print_status 0 "Docker is installed"
else
    print_status 1 "Docker is not installed"
    exit 1
fi

# Check Docker Compose
if command -v docker compose &> /dev/null || docker compose version &> /dev/null; then
    print_status 0 "Docker Compose is available"
else
    print_status 1 "Docker Compose is not available"
    exit 1
fi

# Check if .env file exists
if [ -f ".env" ]; then
    print_status 0 ".env file exists"
    
    # Check if BWS credentials are set
    if grep -q "BWS_CLIENT_ID=your_" .env || grep -q "BWS_KEY=your_" .env; then
        print_warning ".env file contains placeholder values - update with real BioID credentials"
    else
        print_status 0 "BioID credentials appear to be configured"
    fi
else
    print_warning ".env file not found - copying from .env.example"
    cp .env.example .env
    print_warning "Please edit .env with your BioID credentials before continuing"
fi

# Check if extension JAR exists
if [ -f "deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar" ]; then
    print_status 0 "Extension JAR exists"
else
    print_status 1 "Extension JAR not found - run 'mvn clean package' first"
    exit 1
fi

echo ""
echo "Starting Docker Compose services..."

# Start services
if docker compose up -d; then
    print_status 0 "Docker Compose services started"
else
    print_status 1 "Failed to start Docker Compose services"
    exit 1
fi

echo ""
echo "Waiting for services to be ready..."

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL..."
for i in {1..30}; do
    if docker compose exec -T postgres pg_isready -U keycloak &> /dev/null; then
        echo ""
        print_status 0 "PostgreSQL is ready"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for Keycloak
echo -n "Waiting for Keycloak..."
for i in {1..60}; do
    if curl -s http://localhost:8080/health/ready &> /dev/null; then
        echo ""
        print_status 0 "Keycloak is ready"
        break
    fi
    echo -n "."
    sleep 3
done

echo ""
echo "Testing endpoints..."

# Test Keycloak health
if curl -s http://localhost:8080/health &> /dev/null; then
    print_status 0 "Keycloak health endpoint responding"
else
    print_status 1 "Keycloak health endpoint not responding"
fi

# Test Keycloak admin console
if curl -s http://localhost:8080/admin/ &> /dev/null; then
    print_status 0 "Keycloak admin console accessible"
else
    print_status 1 "Keycloak admin console not accessible"
fi

# Test metrics endpoint
if curl -s http://localhost:9000/metrics &> /dev/null; then
    print_status 0 "Metrics endpoint responding"
else
    print_status 1 "Metrics endpoint not responding"
fi

# Check if extension is loaded
if docker compose logs keycloak | grep -q "BioID"; then
    print_status 0 "BioID extension appears to be loaded"
else
    print_warning "BioID extension may not be loaded - check logs"
fi

echo ""
echo "Service Status:"
docker compose ps

echo ""
echo "🎉 Setup test completed!"
echo ""
echo "Next steps:"
echo "1. Access Keycloak Admin Console: http://localhost:8080/admin"
echo "2. Login with: admin / admin123"
echo "3. Create a realm and configure face authentication"
echo "4. See SETUP.md for detailed configuration instructions"
echo ""
echo "To view logs: docker compose logs -f keycloak"
echo "To stop services: docker compose down"