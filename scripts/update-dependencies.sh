#!/bin/bash

# Dependency Update Script
# This script helps safely update dependencies with proper testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to prompt for confirmation
confirm() {
    read -p "$1 (y/N): " -n 1 -r
    echo
    [[ $REPLY =~ ^[Yy]$ ]]
}

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "No pom.xml found. Please run this script from the project root."
    exit 1
fi

print_status "🔄 Dependency Update Assistant"
echo

# Create backup
print_status "Creating backup of current pom.xml..."
cp pom.xml pom.xml.backup
print_success "Backup created: pom.xml.backup"

# Show available updates
print_status "Checking for available updates..."
mvn versions:display-dependency-updates

echo
print_warning "⚠️  IMPORTANT: Always test thoroughly after updating dependencies!"
echo

# Ask what type of update to perform
echo "Select update type:"
echo "1) Update all dependencies to latest versions (⚠️  RISKY)"
echo "2) Update only patch versions (safer)"
echo "3) Update specific dependency"
echo "4) Update only security-critical dependencies"
echo "5) Show current versions and exit"
echo

read -p "Enter your choice (1-5): " choice

case $choice in
    1)
        if confirm "⚠️  This will update ALL dependencies to latest versions. This can break compatibility. Continue?"; then
            print_status "Updating all dependencies..."
            mvn versions:use-latest-versions
            print_warning "All dependencies updated. MUST test thoroughly!"
        else
            print_status "Update cancelled."
            exit 0
        fi
        ;;
    2)
        print_status "Updating only patch versions (safer)..."
        mvn versions:use-latest-versions -DallowMajorUpdates=false -DallowMinorUpdates=false
        print_success "Patch versions updated."
        ;;
    3)
        echo "Common dependencies to update:"
        echo "- keycloak.version"
        echo "- grpc.version"
        echo "- protobuf.version"
        echo "- jjwt.version"
        echo "- bouncycastle.version"
        echo "- junit.version"
        echo "- mockito.version"
        echo
        read -p "Enter property name (e.g., keycloak.version): " property
        read -p "Enter new version: " version
        
        if confirm "Update $property to $version?"; then
            mvn versions:set-property -Dproperty=$property -DnewVersion=$version
            print_success "Updated $property to $version"
        else
            print_status "Update cancelled."
            exit 0
        fi
        ;;
    4)
        print_status "Updating security-critical dependencies..."
        
        # List of security-critical dependencies
        SECURITY_DEPS=("keycloak.version" "jjwt.version" "bouncycastle.version" "jackson.version")
        
        for dep in "${SECURITY_DEPS[@]}"; do
            print_status "Checking $dep..."
            # This would need more sophisticated logic to determine latest secure version
            echo "Manual review required for $dep"
        done
        
        print_warning "Security updates require manual review. Check CVE databases for each dependency."
        ;;
    5)
        print_status "Current dependency versions:"
        mvn help:evaluate -Dexpression=project.properties -q -DforceStdout | grep -E "version>"
        exit 0
        ;;
    *)
        print_error "Invalid choice. Exiting."
        exit 1
        ;;
esac

# Test the updates
echo
print_status "🧪 Testing updated dependencies..."

# Clean compile
print_status "1. Testing compilation..."
if mvn clean compile -q; then
    print_success "✅ Compilation successful"
else
    print_error "❌ Compilation failed"
    if confirm "Compilation failed. Restore backup?"; then
        cp pom.xml.backup pom.xml
        print_success "Backup restored"
    fi
    exit 1
fi

# Run tests
print_status "2. Running unit tests..."
if mvn test -q; then
    print_success "✅ Unit tests passed"
else
    print_error "❌ Unit tests failed"
    if confirm "Tests failed. Restore backup?"; then
        cp pom.xml.backup pom.xml
        print_success "Backup restored"
    fi
    exit 1
fi

# Security scan
print_status "3. Running security scan..."
if mvn org.owasp:dependency-check-maven:check -q; then
    print_success "✅ No new security vulnerabilities"
else
    print_warning "⚠️  New security vulnerabilities detected. Review target/dependency-check-report.html"
fi

# Generate updated dependency report
print_status "4. Generating updated dependency report..."
mvn versions:display-dependency-updates > updated-dependencies.log

print_success "🎉 Dependency update completed successfully!"

echo
echo "📋 Summary:"
echo "- Backup created: pom.xml.backup"
echo "- Updated dependencies tested successfully"
echo "- Security scan completed"
echo "- Updated dependency report: updated-dependencies.log"
echo

print_status "🔧 Next steps:"
echo "1. Review changes: git diff pom.xml"
echo "2. Run integration tests: mvn verify -Pit"
echo "3. Test Docker build: docker compose build"
echo "4. Commit changes: git add pom.xml && git commit -m 'Update dependencies'"
echo "5. Remove backup: rm pom.xml.backup"

echo
if confirm "Remove backup file now?"; then
    rm pom.xml.backup
    print_success "Backup file removed"
else
    print_status "Backup file kept: pom.xml.backup"
fi

print_success "Dependency update process completed! 🚀"