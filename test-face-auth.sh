#!/bin/bash

# BioID Face Authentication Test Script
# This script helps verify that your setup is working correctly

set -e

echo "🧪 BioID Face Authentication Test Script"
echo "========================================"
echo

# Check if Keycloak is running
echo "1. Checking Keycloak availability..."
if curl -s -f http://localhost:8080/health/ready > /dev/null; then
    echo "✅ Keycloak is running and ready"
else
    echo "❌ Keycloak is not running or not ready"
    echo "   Run: docker compose up -d"
    exit 1
fi

# Check if bioid-demo realm exists
echo
echo "2. Checking if bioid-demo realm exists..."
if curl -s -f http://localhost:8080/realms/bioid-demo/.well-known/openid_configuration > /dev/null; then
    echo "✅ bioid-demo realm is accessible"
else
    echo "❌ bioid-demo realm not found"
    echo "   You need to create the realm manually:"
    echo "   1. Go to http://localhost:8080/admin"
    echo "   2. Login with admin/admin123"
    echo "   3. Create Realm → Browse → Select docker/keycloak/bioid-demo-realm.json"
    echo "   Or follow the manual setup guide in MANUAL_SETUP.md"
    exit 1
fi

# Check if BioID extension is loaded
echo
echo "3. Checking BioID extension..."
if docker compose logs keycloak 2>/dev/null | grep -q "BioID.*initialized"; then
    echo "✅ BioID extension is loaded and initialized"
else
    echo "⚠️  BioID extension may not be properly initialized"
    echo "   Check logs: docker compose logs keycloak | grep -i bioid"
fi

# Check if test app files exist
echo
echo "4. Checking test application files..."
if [ -f "test-app.html" ]; then
    echo "✅ test-app.html found"
else
    echo "❌ test-app.html not found"
    exit 1
fi

if [ -f "serve-test-app.py" ]; then
    echo "✅ serve-test-app.py found"
else
    echo "❌ serve-test-app.py not found"
    exit 1
fi

# Check Python availability
echo
echo "5. Checking Python for test server..."
if command -v python3 &> /dev/null; then
    echo "✅ Python 3 is available"
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    echo "✅ Python is available"
    PYTHON_CMD="python"
else
    echo "⚠️  Python not found - you can still open test-app.html directly in your browser"
    PYTHON_CMD=""
fi

echo
echo "🎉 Setup verification complete!"
echo
echo "Next steps:"
echo "1. Start the test server:"
if [ -n "$PYTHON_CMD" ]; then
    echo "   $PYTHON_CMD serve-test-app.py"
else
    echo "   Open test-app.html directly in your browser"
fi
echo
echo "2. Open http://localhost:3000/test-app.html (if using test server)"
echo
echo "3. Click 'Login with Face Authentication' and test the flow"
echo
echo "4. For account management, go to: http://localhost:8080/realms/bioid-demo/account"
echo
echo "Troubleshooting:"
echo "- If you see 'Invalid redirect URI', make sure the client allows http://localhost:3000/*"
echo "- If account management doesn't work, try the manual setup in MANUAL_SETUP.md"
echo "- Check logs with: docker compose logs keycloak | grep -i bioid"