#!/bin/bash

# Production Setup Verification Script for BioID Keycloak Extension
# This script verifies that the system is properly configured for production use

set -e

echo "🔍 BioID Keycloak Extension - Production Setup Verification"
echo "=========================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# Function to print error messages
error() {
    echo -e "${RED}❌ ERROR: $1${NC}"
    ((ERRORS++))
}

# Function to print warning messages
warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
    ((WARNINGS++))
}

# Function to print success messages
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print info messages
info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

echo
echo "1. Checking Environment Configuration..."
echo "----------------------------------------"

# Check for required environment variables
if [ -z "$BWS_CLIENT_ID" ]; then
    error "BWS_CLIENT_ID environment variable is not set"
    info "Set this to your BioID client ID from https://bwsportal.bioid.com/"
else
    if [ ${#BWS_CLIENT_ID} -lt 10 ]; then
        warning "BWS_CLIENT_ID appears to be too short (${#BWS_CLIENT_ID} characters)"
    else
        success "BWS_CLIENT_ID is configured (${#BWS_CLIENT_ID} characters)"
    fi
fi

if [ -z "$BWS_KEY" ]; then
    error "BWS_KEY environment variable is not set"
    info "Set this to your BioID secret key from https://bwsportal.bioid.com/"
else
    if [ ${#BWS_KEY} -lt 16 ]; then
        warning "BWS_KEY appears to be too short (${#BWS_KEY} characters)"
    else
        success "BWS_KEY is configured (${#BWS_KEY} characters)"
    fi
fi

if [ -z "$BWS_ENDPOINT" ]; then
    warning "BWS_ENDPOINT not set, will use default (face.bws-eu.bioid.com)"
    export BWS_ENDPOINT="face.bws-eu.bioid.com"
else
    success "BWS_ENDPOINT is configured: $BWS_ENDPOINT"
    
    # Validate endpoint format
    if [[ $BWS_ENDPOINT == http://* ]]; then
        error "BWS_ENDPOINT uses HTTP instead of HTTPS - this is not secure"
    elif [[ $BWS_ENDPOINT == *bioid.com ]]; then
        success "BWS_ENDPOINT appears to be a valid BioID endpoint"
    else
        warning "BWS_ENDPOINT doesn't appear to be a standard BioID endpoint"
    fi
fi

echo
echo "2. Checking Network Connectivity..."
echo "-----------------------------------"

# Test network connectivity to BioID service
ENDPOINT_HOST=$(echo $BWS_ENDPOINT | sed 's|grpcs://||' | sed 's|:.*||')
if command -v nc >/dev/null 2>&1; then
    if nc -z -w5 $ENDPOINT_HOST 443 2>/dev/null; then
        success "Network connectivity to $ENDPOINT_HOST:443 is working"
    else
        error "Cannot connect to $ENDPOINT_HOST:443 - check network connectivity and firewall settings"
    fi
else
    warning "netcat (nc) not available - cannot test network connectivity"
fi

echo
echo "3. Checking Java Environment..."
echo "-------------------------------"

if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    success "Java is available: $JAVA_VERSION"
    
    # Check Java version (should be 11 or higher for Keycloak)
    JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
    if [ "$JAVA_MAJOR" -ge 11 ]; then
        success "Java version is compatible with Keycloak"
    else
        error "Java version $JAVA_VERSION is too old - Keycloak requires Java 11 or higher"
    fi
else
    error "Java is not available in PATH"
fi

echo
echo "4. Checking File Permissions..."
echo "-------------------------------"

# Check if configuration files exist and are readable
if [ -f ".env" ]; then
    if [ -r ".env" ]; then
        success ".env file exists and is readable"
    else
        error ".env file exists but is not readable"
    fi
else
    warning ".env file not found - using environment variables or defaults"
fi

if [ -f "bioid.properties" ]; then
    if [ -r "bioid.properties" ]; then
        success "bioid.properties file exists and is readable"
    else
        error "bioid.properties file exists but is not readable"
    fi
else
    info "bioid.properties file not found - using environment variables or defaults"
fi

echo
echo "5. Checking Docker Environment (if applicable)..."
echo "------------------------------------------------"

if command -v docker >/dev/null 2>&1; then
    if docker info >/dev/null 2>&1; then
        success "Docker is available and running"
        
        # Check if docker-compose is available
        if command -v docker-compose >/dev/null 2>&1; then
            success "Docker Compose is available"
        else
            warning "Docker Compose not found - you may need it for containerized deployment"
        fi
    else
        warning "Docker is installed but not running or not accessible"
    fi
else
    info "Docker not found - assuming non-containerized deployment"
fi

echo
echo "6. Security Recommendations..."
echo "------------------------------"

# Check for common security issues
if [ -f ".env" ] && grep -q "admin123" ".env" 2>/dev/null; then
    warning "Default admin password detected in .env file - change this for production"
fi

if [ -f "docker-compose.yml" ] && grep -q "admin123" "docker-compose.yml" 2>/dev/null; then
    warning "Default admin password detected in docker-compose.yml - change this for production"
fi

# Check file permissions on sensitive files
for file in .env bioid.properties; do
    if [ -f "$file" ]; then
        PERMS=$(stat -c "%a" "$file" 2>/dev/null || stat -f "%A" "$file" 2>/dev/null || echo "unknown")
        if [ "$PERMS" != "600" ] && [ "$PERMS" != "unknown" ]; then
            warning "$file has permissions $PERMS - consider restricting to 600 for better security"
        fi
    fi
done

success "Security check completed"

echo
echo "7. Production Readiness Summary..."
echo "---------------------------------"

if [ $ERRORS -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}🎉 PRODUCTION READY: All checks passed successfully!${NC}"
        echo -e "${GREEN}Your BioID Keycloak extension is properly configured for production use.${NC}"
    else
        echo -e "${YELLOW}⚠️  PRODUCTION READY WITH WARNINGS: $WARNINGS warning(s) found${NC}"
        echo -e "${YELLOW}The system will work but consider addressing the warnings above.${NC}"
    fi
else
    echo -e "${RED}❌ NOT PRODUCTION READY: $ERRORS error(s) found${NC}"
    echo -e "${RED}Fix the errors above before deploying to production.${NC}"
    exit 1
fi

echo
echo "Next Steps:"
echo "----------"
echo "1. Start your Keycloak server with the BioID extension"
echo "2. Monitor logs for any 'PRODUCTION ISSUE' messages"
echo "3. Test face enrollment and authentication with real users"
echo "4. Set up monitoring and alerting for the BioID service"
echo "5. Review and update your backup and disaster recovery procedures"

echo
echo "For support and documentation, visit:"
echo "- BioID Portal: https://bwsportal.bioid.com/"
echo "- BioID Documentation: https://developer.bioid.com/"

exit 0