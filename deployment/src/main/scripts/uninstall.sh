#!/bin/bash

# Keycloak BioID Extension Uninstallation Script
# This script removes the BioID Face Recognition Extension from Keycloak

set -e

# Configuration
EXTENSION_VERSION="${project.version}"
EXTENSION_JAR="keycloak-bioid-extension-${EXTENSION_VERSION}.jar"
CONFIG_FILE="bioid.properties"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to detect Keycloak installation
detect_keycloak() {
    if [ -n "$KEYCLOAK_HOME" ] && [ -d "$KEYCLOAK_HOME" ]; then
        echo "$KEYCLOAK_HOME"
        return 0
    fi
    
    # Common Keycloak installation paths
    local common_paths=(
        "/opt/keycloak"
        "/usr/local/keycloak"
        "/home/keycloak"
        "$HOME/keycloak"
    )
    
    for path in "${common_paths[@]}"; do
        if [ -d "$path" ] && [ -f "$path/bin/kc.sh" ]; then
            echo "$path"
            return 0
        fi
    done
    
    return 1
}

# Function to validate Keycloak installation
validate_keycloak() {
    local kc_home="$1"
    
    if [ ! -d "$kc_home" ]; then
        log_error "Keycloak directory not found: $kc_home"
        return 1
    fi
    
    if [ ! -f "$kc_home/bin/kc.sh" ]; then
        log_error "Keycloak executable not found: $kc_home/bin/kc.sh"
        return 1
    fi
    
    return 0
}

# Function to check if extension is installed
check_installation() {
    local kc_home="$1"
    local jar_path="$kc_home/providers/$EXTENSION_JAR"
    local config_path="$kc_home/conf/$CONFIG_FILE"
    
    local found=0
    
    if [ -f "$jar_path" ]; then
        log_info "Found extension JAR: $jar_path"
        ((found++))
    fi
    
    if [ -f "$config_path" ]; then
        log_info "Found configuration file: $config_path"
        ((found++))
    fi
    
    # Check for any BioID extension JARs (different versions)
    local bioid_jars=$(find "$kc_home/providers" -name "keycloak-bioid-extension-*.jar" 2>/dev/null | wc -l)
    if [ "$bioid_jars" -gt 0 ]; then
        log_info "Found $bioid_jars BioID extension JAR(s) in providers directory"
        ((found++))
    fi
    
    return $found
}

# Function to backup before uninstall
backup_before_uninstall() {
    local kc_home="$1"
    local backup_dir="$kc_home/backup/bioid-extension-uninstall-$(date +%Y%m%d-%H%M%S)"
    
    log_info "Creating backup before uninstall..."
    mkdir -p "$backup_dir"
    
    local backed_up=0
    
    # Backup extension JARs
    find "$kc_home/providers" -name "keycloak-bioid-extension-*.jar" -exec cp {} "$backup_dir/" \; 2>/dev/null || true
    if [ "$(ls -A "$backup_dir"/*.jar 2>/dev/null | wc -l)" -gt 0 ]; then
        log_info "Backed up extension JAR(s) to $backup_dir/"
        ((backed_up++))
    fi
    
    # Backup configuration
    if [ -f "$kc_home/conf/$CONFIG_FILE" ]; then
        cp "$kc_home/conf/$CONFIG_FILE" "$backup_dir/"
        log_info "Backed up configuration to $backup_dir/"
        ((backed_up++))
    fi
    
    if [ $backed_up -gt 0 ]; then
        log_success "Backup created at $backup_dir"
    else
        rmdir "$backup_dir" 2>/dev/null || true
        log_info "No files to backup"
    fi
}

# Function to remove extension files
remove_extension_files() {
    local kc_home="$1"
    local removed=0
    
    log_info "Removing BioID extension files..."
    
    # Remove specific version JAR
    local jar_path="$kc_home/providers/$EXTENSION_JAR"
    if [ -f "$jar_path" ]; then
        rm -f "$jar_path"
        if [ ! -f "$jar_path" ]; then
            log_success "Removed extension JAR: $jar_path"
            ((removed++))
        else
            log_error "Failed to remove extension JAR: $jar_path"
        fi
    fi
    
    # Remove any other BioID extension JARs
    local other_jars=$(find "$kc_home/providers" -name "keycloak-bioid-extension-*.jar" 2>/dev/null)
    if [ -n "$other_jars" ]; then
        echo "$other_jars" | while read -r jar; do
            rm -f "$jar"
            if [ ! -f "$jar" ]; then
                log_success "Removed extension JAR: $jar"
                ((removed++))
            else
                log_error "Failed to remove extension JAR: $jar"
            fi
        done
    fi
    
    return $removed
}

# Function to remove configuration
remove_configuration() {
    local kc_home="$1"
    local config_path="$kc_home/conf/$CONFIG_FILE"
    
    if [ -f "$config_path" ]; then
        read -p "Remove configuration file $config_path? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rm -f "$config_path"
            if [ ! -f "$config_path" ]; then
                log_success "Removed configuration file: $config_path"
                return 0
            else
                log_error "Failed to remove configuration file: $config_path"
                return 1
            fi
        else
            log_info "Configuration file preserved: $config_path"
            return 0
        fi
    else
        log_info "No configuration file found to remove"
        return 0
    fi
}

# Function to clean up empty directories
cleanup_directories() {
    local kc_home="$1"
    
    log_info "Cleaning up empty directories..."
    
    # Remove backup directory if empty
    if [ -d "$kc_home/backup" ]; then
        find "$kc_home/backup" -type d -empty -delete 2>/dev/null || true
    fi
    
    log_success "Directory cleanup completed"
}

# Function to verify uninstallation
verify_uninstallation() {
    local kc_home="$1"
    
    log_info "Verifying uninstallation..."
    
    local remaining=0
    
    # Check for remaining JAR files
    local remaining_jars=$(find "$kc_home/providers" -name "keycloak-bioid-extension-*.jar" 2>/dev/null | wc -l)
    if [ "$remaining_jars" -gt 0 ]; then
        log_warning "$remaining_jars BioID extension JAR(s) still present"
        ((remaining++))
    else
        log_success "No BioID extension JARs found"
    fi
    
    # Check for configuration file
    if [ -f "$kc_home/conf/$CONFIG_FILE" ]; then
        log_info "Configuration file preserved: $kc_home/conf/$CONFIG_FILE"
    else
        log_success "Configuration file removed"
    fi
    
    if [ $remaining -eq 0 ]; then
        log_success "Uninstallation verification completed successfully"
        return 0
    else
        log_warning "Some files may still be present"
        return 1
    fi
}

# Function to display post-uninstall instructions
show_post_uninstall_instructions() {
    local kc_home="$1"
    
    echo
    log_success "Uninstallation completed!"
    echo
    echo "Next steps:"
    echo "1. Restart Keycloak to unload the extension:"
    echo "   $kc_home/bin/kc.sh start"
    echo
    echo "2. Remove face authentication from your Keycloak realm configuration:"
    echo "   - Navigate to Authentication → Flows"
    echo "   - Remove Face Authenticator from authentication flows"
    echo "   - Disable Face Enrollment required action"
    echo
    echo "3. Clean up user face credentials if needed:"
    echo "   - Users will need to re-enroll if you reinstall the extension"
    echo "   - Face credential data will be automatically cleaned up"
    echo
    echo "4. Review and clean up any custom configuration or integration"
    echo
    echo "Backup files are preserved in the backup directory for recovery if needed."
}

# Main uninstallation function
main() {
    echo "Keycloak BioID Face Recognition Extension Uninstaller"
    echo "Version: $EXTENSION_VERSION"
    echo "====================================================="
    echo
    
    # Detect Keycloak installation
    log_info "Detecting Keycloak installation..."
    
    local keycloak_home
    if ! keycloak_home=$(detect_keycloak); then
        log_error "Keycloak installation not found"
        echo "Please set KEYCLOAK_HOME environment variable or install Keycloak in a standard location"
        exit 1
    fi
    
    log_success "Keycloak detected at: $keycloak_home"
    
    # Validate Keycloak installation
    if ! validate_keycloak "$keycloak_home"; then
        exit 1
    fi
    
    # Check if extension is installed
    log_info "Checking for BioID extension installation..."
    
    if ! check_installation "$keycloak_home"; then
        log_warning "No BioID extension installation found"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Uninstallation cancelled"
            exit 0
        fi
    fi
    
    # Confirm uninstallation
    echo
    log_warning "This will remove the BioID Face Recognition Extension from Keycloak"
    log_warning "Users will lose access to face authentication until the extension is reinstalled"
    echo
    read -p "Continue with uninstallation? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Uninstallation cancelled"
        exit 0
    fi
    
    # Backup before uninstall
    backup_before_uninstall "$keycloak_home"
    
    # Remove extension files
    remove_extension_files "$keycloak_home"
    
    # Remove configuration (with confirmation)
    remove_configuration "$keycloak_home"
    
    # Clean up directories
    cleanup_directories "$keycloak_home"
    
    # Verify uninstallation
    verify_uninstallation "$keycloak_home"
    
    # Show post-uninstall instructions
    show_post_uninstall_instructions "$keycloak_home"
}

# Run main function
main "$@"