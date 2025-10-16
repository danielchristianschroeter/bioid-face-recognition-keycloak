#!/bin/bash

# Dependency Update Check Script
# This script checks for outdated dependencies and security vulnerabilities

set -e

echo "🔍 Checking for dependency updates..."

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

print_status "Starting dependency analysis..."

# 1. Check for dependency updates
print_status "Checking for dependency updates..."
mvn versions:display-dependency-updates | tee dependency-updates.log

# 2. Check for plugin updates
print_status "Checking for plugin updates..."
mvn versions:display-plugin-updates | tee plugin-updates.log

# 3. Check for property updates
print_status "Checking for property updates..."
mvn versions:display-property-updates | tee property-updates.log

# 4. Run security vulnerability scan
print_status "Running security vulnerability scan..."
if mvn org.owasp:dependency-check-maven:check -q; then
    print_success "No critical security vulnerabilities found"
else
    print_warning "Security vulnerabilities detected. Check the report in target/dependency-check-report.html"
fi

# 5. Analyze dependencies for unused/undeclared
print_status "Analyzing dependency usage..."
mvn dependency:analyze | tee dependency-analysis.log

# 6. Check for dependency conflicts
print_status "Checking for dependency conflicts..."
mvn dependency:tree | grep -E "\[WARNING\]|\[ERROR\]" || print_success "No dependency conflicts detected"

# 7. Generate summary report
print_status "Generating summary report..."

echo "
=== DEPENDENCY UPDATE SUMMARY ===
Generated on: $(date)

📊 Reports Generated:
- dependency-updates.log: Available dependency updates
- plugin-updates.log: Available plugin updates  
- property-updates.log: Available property updates
- dependency-analysis.log: Dependency usage analysis
- target/dependency-check-report.html: Security vulnerability report

🔧 Next Steps:
1. Review the generated logs for available updates
2. Check target/dependency-check-report.html for security issues
3. Update dependencies using: ./scripts/update-dependencies.sh
4. Test thoroughly after updates

📋 Quick Commands:
- Update all dependencies: mvn versions:use-latest-versions
- Update specific property: mvn versions:set-property -Dproperty=VERSION_NAME -DnewVersion=NEW_VERSION
- Revert changes: mvn versions:revert

⚠️  Always test after updating dependencies!
" | tee dependency-summary.txt

print_success "Dependency check completed. Review the summary above and generated files."

# Check if there are any critical updates needed
if grep -q "CRITICAL" dependency-updates.log 2>/dev/null; then
    print_warning "Critical updates available! Review dependency-updates.log"
    exit 1
fi

print_success "Dependency check completed successfully!"