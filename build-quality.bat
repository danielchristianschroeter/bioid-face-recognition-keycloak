@echo off
REM Build script with comprehensive quality checks for BioID Keycloak Extension
REM This script runs all quality gates and generates reports

setlocal enabledelayedexpansion

echo 🚀 Starting comprehensive build with quality checks...

REM Function to print status messages
set "INFO_COLOR=[94m"
set "SUCCESS_COLOR=[92m"
set "WARNING_COLOR=[93m"
set "ERROR_COLOR=[91m"
set "RESET_COLOR=[0m"

REM Check prerequisites
echo %INFO_COLOR%[INFO]%RESET_COLOR% Checking prerequisites...

where mvn >nul 2>&1
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Maven is not installed or not in PATH
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Java is not installed or not in PATH
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION_STRING=%%g
)
set JAVA_VERSION_STRING=%JAVA_VERSION_STRING:"=%
for /f "delims=." %%a in ("%JAVA_VERSION_STRING%") do set JAVA_MAJOR=%%a

if %JAVA_MAJOR% LSS 21 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Java 21 or higher is required. Current version: %JAVA_MAJOR%
    exit /b 1
)

echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Prerequisites check passed

REM Clean previous builds
echo %INFO_COLOR%[INFO]%RESET_COLOR% Cleaning previous builds...
call mvn clean -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Clean failed
    exit /b 1
)
echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Clean completed

REM Compile and run tests
echo %INFO_COLOR%[INFO]%RESET_COLOR% Compiling and running tests...
call mvn compile test -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Compilation or tests failed
    exit /b 1
)
echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Compilation and tests passed

REM Code formatting check
echo %INFO_COLOR%[INFO]%RESET_COLOR% Checking code formatting...
call mvn spotless:check -q
if errorlevel 1 (
    echo %WARNING_COLOR%[WARNING]%RESET_COLOR% Code formatting issues found. Run 'mvn spotless:apply' to fix
    if "%1"=="--fix" (
        echo %INFO_COLOR%[INFO]%RESET_COLOR% Auto-fixing code formatting...
        call mvn spotless:apply -q
        echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Code formatting fixed
    )
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Code formatting check passed
)

REM Static analysis with Checkstyle
echo %INFO_COLOR%[INFO]%RESET_COLOR% Running Checkstyle analysis...
call mvn checkstyle:check -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Checkstyle violations found. Check target\checkstyle-result.xml
    if not "%1"=="--continue-on-error" exit /b 1
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Checkstyle analysis passed
)

REM Static analysis with PMD
echo %INFO_COLOR%[INFO]%RESET_COLOR% Running PMD analysis...
call mvn pmd:check -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% PMD violations found. Check target\pmd.xml
    if not "%1"=="--continue-on-error" exit /b 1
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% PMD analysis passed
)

REM Static analysis with SpotBugs
echo %INFO_COLOR%[INFO]%RESET_COLOR% Running SpotBugs analysis...
call mvn spotbugs:check -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% SpotBugs violations found. Check target\spotbugsXml.xml
    if not "%1"=="--continue-on-error" exit /b 1
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% SpotBugs analysis passed
)

REM Security scan with OWASP Dependency Check
echo %INFO_COLOR%[INFO]%RESET_COLOR% Running OWASP Dependency Check...
call mvn org.owasp:dependency-check-maven:check -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Security vulnerabilities found. Check target\dependency-check-report.html
    if not "%1"=="--continue-on-error" exit /b 1
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% OWASP Dependency Check passed
)

REM Generate test coverage report
echo %INFO_COLOR%[INFO]%RESET_COLOR% Generating test coverage report...
call mvn jacoco:report -q
if errorlevel 1 (
    echo %WARNING_COLOR%[WARNING]%RESET_COLOR% Failed to generate test coverage report
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Test coverage report generated
)

REM Package the application
echo %INFO_COLOR%[INFO]%RESET_COLOR% Packaging application...
call mvn package -DskipTests -q
if errorlevel 1 (
    echo %ERROR_COLOR%[ERROR]%RESET_COLOR% Packaging failed
    exit /b 1
)
echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Packaging completed

REM Generate documentation
echo %INFO_COLOR%[INFO]%RESET_COLOR% Generating documentation...
call mvn javadoc:javadoc -q
if errorlevel 1 (
    echo %WARNING_COLOR%[WARNING]%RESET_COLOR% Documentation generation had issues
) else (
    echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% Documentation generated
)

REM Summary
echo.
echo 📊 Build Quality Report Summary:
echo ================================

REM Check if reports exist and provide links
if exist "target\checkstyle-result.xml" (
    echo ✅ Checkstyle Report: target\checkstyle-result.xml
)

if exist "target\pmd.xml" (
    echo ✅ PMD Report: target\pmd.xml
)

if exist "target\spotbugsXml.xml" (
    echo ✅ SpotBugs Report: target\spotbugsXml.xml
)

if exist "target\dependency-check-report.html" (
    echo ✅ OWASP Dependency Check: target\dependency-check-report.html
)

if exist "target\site\jacoco\index.html" (
    echo ✅ Test Coverage Report: target\site\jacoco\index.html
)

if exist "target\site\apidocs\index.html" (
    echo ✅ JavaDoc Documentation: target\site\apidocs\index.html
)

echo.
echo %SUCCESS_COLOR%[SUCCESS]%RESET_COLOR% 🎉 Build completed successfully with quality checks!

REM Optional: Open reports in browser (Windows)
if "%1"=="--open-reports" (
    if exist "target\site\jacoco\index.html" start "" "target\site\jacoco\index.html"
    if exist "target\dependency-check-report.html" start "" "target\dependency-check-report.html"
)

echo.
echo Usage options:
echo   build-quality.bat                    # Standard build with quality checks
echo   build-quality.bat --fix              # Auto-fix formatting issues
echo   build-quality.bat --continue-on-error # Continue even if quality checks fail
echo   build-quality.bat --open-reports     # Open reports in browser after build

endlocal