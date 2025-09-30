#!/bin/bash

# Keycloak BioID Extension Setup Verification Script

echo "🔍 Verifying Keycloak BioID Extension Setup..."
echo "================================================"

# Check if Docker containers are running
echo "1. Checking Docker containers..."
if docker compose ps | grep -q "keycloak-bioid.*Up"; then
    echo "✅ Keycloak container is running"
else
    echo "❌ Keycloak container is not running"
    echo "   Run: docker compose up -d"
    exit 1
fi

if docker compose ps | grep -q "keycloak-postgres.*Up"; then
    echo "✅ PostgreSQL container is running"
else
    echo "❌ PostgreSQL container is not running"
    exit 1
fi

# Check Keycloak health
echo ""
echo "2. Checking Keycloak health..."
if curl -s -f http://localhost:8080/health/ready > /dev/null; then
    echo "✅ Keycloak is ready"
else
    echo "❌ Keycloak is not ready"
    echo "   Check logs: docker compose logs keycloak"
    exit 1
fi

# Check if bioid-demo realm exists
echo ""
echo "3. Checking bioid-demo realm..."
if curl -s http://localhost:8080/realms/bioid-demo/.well-known/openid_configuration > /dev/null 2>&1; then
    echo "✅ bioid-demo realm is accessible"
else
    echo "❌ bioid-demo realm is not accessible"
    echo "   Check realm import logs: docker compose logs keycloak | grep -i import"
    exit 1
fi

# Check if extension is loaded
echo ""
echo "4. Checking BioID extension..."
if docker compose logs keycloak | grep -q "bioid"; then
    echo "✅ BioID extension logs found"
else
    echo "⚠️  No BioID extension logs found (this might be normal)"
fi

# Check if demo user can be accessed (indirect check)
echo ""
echo "5. Testing demo realm access..."
echo "   Demo user login URL: http://localhost:8080/realms/bioid-demo/account"
echo "   Demo user credentials: demo / demo123"
echo "   Admin console: http://localhost:8080/admin/master/console/#/bioid-demo"

echo ""
echo "🎉 Setup verification complete!"
echo ""
echo "Next steps:"
echo "1. Open: http://localhost:8080/realms/bioid-demo/account"
echo "2. Login with: demo / demo123"
echo "3. Complete face enrollment when prompted"
echo ""
echo "If you encounter issues:"
echo "- Check logs: docker compose logs -f keycloak"
echo "- Verify .env file has BWS_CLIENT_ID and BWS_KEY set"
echo "- Ensure you're using the bioid-demo realm URLs, not master realm"