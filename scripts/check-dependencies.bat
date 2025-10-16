@echo off
REM Dependency Update Check Script for Windows
REM This script checks for outdated dependencies and security vulnerabilities

setlocal enabledelayedexpansion

echo 🔍 Checking for dependency updates...

REM Check if Maven is available
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    exit /b 1
)

REM Check if we're in the right directory
if not exist "pom.xml" (
    echo [ERROR] No pom.xml found. Please run this script from the project root.
    exit /b 1
)

echo [INFO] Starting dependency analysis...

REM 1. Check for dependency updates
echo [INFO] Checking for dependency updates...
call mvn versions:display-dependency-updates > dependency-updates.log 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Failed to check dependency updates
)

REM 2. Check for plugin updates
echo [INFO] Checking for plugin updates...
call mvn versions:display-plugin-updates > plugin-updates.log 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Failed to check plugin updates
)

REM 3. Check for property updates
echo [INFO] Checking for property updates...
call mvn versions:display-property-updates > property-updates.log 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Failed to check property updates
)

REM 4. Run security vulnerability scan
echo [INFO] Running security vulnerability scan...
call mvn org.owasp:dependency-check-maven:check -q
if %errorlevel% equ 0 (
    echo [SUCCESS] No critical security vulnerabilities found
) else (
    echo [WARNING] Security vulnerabilities detected. Check the report in target\dependency-check-report.html
)

REM 5. Analyze dependencies for unused/undeclared
echo [INFO] Analyzing dependency usage...
call mvn dependency:analyze > dependency-analysis.log 2>&1

REM 6. Generate summary report
echo [INFO] Generating summary report...

echo.
echo === DEPENDENCY UPDATE SUMMARY ===
echo Generated on: %date% %time%
echo.
echo 📊 Reports Generated:
echo - dependency-updates.log: Available dependency updates
echo - plugin-updates.log: Available plugin updates  
echo - property-updates.log: Available property updates
echo - dependency-analysis.log: Dependency usage analysis
echo - target\dependency-check-report.html: Security vulnerability report
echo.
echo 🔧 Next Steps:
echo 1. Review the generated logs for available updates
echo 2. Check target\dependency-check-report.html for security issues
echo 3. Update dependencies using: scripts\update-dependencies.bat
echo 4. Test thoroughly after updates
echo.
echo 📋 Quick Commands:
echo - Update all dependencies: mvn versions:use-latest-versions
echo - Update specific property: mvn versions:set-property -Dproperty=VERSION_NAME -DnewVersion=NEW_VERSION
echo - Revert changes: mvn versions:revert
echo.
echo ⚠️  Always test after updating dependencies!

echo [SUCCESS] Dependency check completed. Review the summary above and generated files.

REM Check if there are any critical updates needed
findstr /C:"CRITICAL" dependency-updates.log >nul 2>nul
if %errorlevel% equ 0 (
    echo [WARNING] Critical updates available! Review dependency-updates.log
    exit /b 1
)

echo [SUCCESS] Dependency check completed successfully!