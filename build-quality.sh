#!/bin/bash

# Build script with comprehensive quality checks for BioID Keycloak Extension
# This script runs all quality gates and generates reports

set -e  # Exit on any error

echo "🚀 Starting comprehensive build with quality checks..."

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

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
print_status "Checking prerequisites..."

if ! command_exists mvn; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

if ! command_exists java; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_error "Java 21 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

print_success "Prerequisites check passed"

# Clean previous builds
print_status "Cleaning previous builds..."
mvn clean -q
print_success "Clean completed"

# Compile and run tests
print_status "Compiling and running tests..."
mvn compile test -q
if [ $? -eq 0 ]; then
    print_success "Compilation and tests passed"
else
    print_error "Compilation or tests failed"
    exit 1
fi

# Code formatting check
print_status "Checking code formatting..."
mvn spotless:check -q
if [ $? -eq 0 ]; then
    print_success "Code formatting check passed"
else
    print_warning "Code formatting issues found. Run 'mvn spotless:apply' to fix"
    # Auto-fix formatting in development mode
    if [ "$1" = "--fix" ]; then
        print_status "Auto-fixing code formatting..."
        mvn spotless:apply -q
        print_success "Code formatting fixed"
    fi
fi

# Static analysis with Checkstyle
print_status "Running Checkstyle analysis..."
mvn checkstyle:check -q
if [ $? -eq 0 ]; then
    print_success "Checkstyle analysis passed"
else
    print_error "Checkstyle violations found. Check target/checkstyle-result.xml"
    if [ "$1" != "--continue-on-error" ]; then
        exit 1
    fi
fi

# Static analysis with PMD
print_status "Running PMD analysis..."
mvn pmd:check -q
if [ $? -eq 0 ]; then
    print_success "PMD analysis passed"
else
    print_error "PMD violations found. Check target/pmd.xml"
    if [ "$1" != "--continue-on-error" ]; then
        exit 1
    fi
fi

# Static analysis with SpotBugs
print_status "Running SpotBugs analysis..."
mvn spotbugs:check -q
if [ $? -eq 0 ]; then
    print_success "SpotBugs analysis passed"
else
    print_error "SpotBugs violations found. Check target/spotbugsXml.xml"
    if [ "$1" != "--continue-on-error" ]; then
        exit 1
    fi
fi

# Security scan with OWASP Dependency Check
print_status "Running OWASP Dependency Check..."
mvn org.owasp:dependency-check-maven:check -q
if [ $? -eq 0 ]; then
    print_success "OWASP Dependency Check passed"
else
    print_error "Security vulnerabilities found. Check target/dependency-check-report.html"
    if [ "$1" != "--continue-on-error" ]; then
        exit 1
    fi
fi

# Generate test coverage report
print_status "Generating test coverage report..."
mvn jacoco:report -q
if [ $? -eq 0 ]; then
    print_success "Test coverage report generated"
    
    # Check coverage threshold (optional)
    if command_exists xmllint; then
        COVERAGE=$(xmllint --xpath "//counter[@type='INSTRUCTION']/@covered" target/site/jacoco/jacoco.xml 2>/dev/null | cut -d'"' -f2)
        TOTAL=$(xmllint --xpath "//counter[@type='INSTRUCTION']/@missed" target/site/jacoco/jacoco.xml 2>/dev/null | cut -d'"' -f2)
        if [ -n "$COVERAGE" ] && [ -n "$TOTAL" ]; then
            COVERAGE_PERCENT=$((COVERAGE * 100 / (COVERAGE + TOTAL)))
            print_status "Test coverage: ${COVERAGE_PERCENT}%"
            if [ "$COVERAGE_PERCENT" -lt 70 ]; then
                print_warning "Test coverage is below 70%"
            fi
        fi
    fi
else
    print_warning "Failed to generate test coverage report"
fi

# Package the application
print_status "Packaging application..."
mvn package -DskipTests -q
if [ $? -eq 0 ]; then
    print_success "Packaging completed"
else
    print_error "Packaging failed"
    exit 1
fi

# Generate documentation
print_status "Generating documentation..."
mvn javadoc:javadoc -q
if [ $? -eq 0 ]; then
    print_success "Documentation generated"
else
    print_warning "Documentation generation had issues"
fi

# Summary
echo ""
echo "📊 Build Quality Report Summary:"
echo "================================"

# Check if reports exist and provide links
if [ -f "target/checkstyle-result.xml" ]; then
    echo "✅ Checkstyle Report: target/checkstyle-result.xml"
fi

if [ -f "target/pmd.xml" ]; then
    echo "✅ PMD Report: target/pmd.xml"
fi

if [ -f "target/spotbugsXml.xml" ]; then
    echo "✅ SpotBugs Report: target/spotbugsXml.xml"
fi

if [ -f "target/dependency-check-report.html" ]; then
    echo "✅ OWASP Dependency Check: target/dependency-check-report.html"
fi

if [ -f "target/site/jacoco/index.html" ]; then
    echo "✅ Test Coverage Report: target/site/jacoco/index.html"
fi

if [ -f "target/site/apidocs/index.html" ]; then
    echo "✅ JavaDoc Documentation: target/site/apidocs/index.html"
fi

echo ""
print_success "🎉 Build completed successfully with quality checks!"

# Optional: Open reports in browser (macOS/Linux)
if [ "$1" = "--open-reports" ]; then
    if command_exists open; then  # macOS
        open target/site/jacoco/index.html 2>/dev/null || true
        open target/dependency-check-report.html 2>/dev/null || true
    elif command_exists xdg-open; then  # Linux
        xdg-open target/site/jacoco/index.html 2>/dev/null || true
        xdg-open target/dependency-check-report.html 2>/dev/null || true
    fi
fi

echo ""
echo "Usage options:"
echo "  ./build-quality.sh                    # Standard build with quality checks"
echo "  ./build-quality.sh --fix              # Auto-fix formatting issues"
echo "  ./build-quality.sh --continue-on-error # Continue even if quality checks fail"
echo "  ./build-quality.sh --open-reports     # Open reports in browser after build"