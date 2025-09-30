#!/bin/bash

# Keycloak BioID Extension Installation Script
# This script automates the installation of the BioID Face Recognition Extension for Keycloak

set -e

# Configuration
EXTENSION_VERSION="${project.version}"
EXTENSION_JAR="keycloak-bioid-extension-${EXTENSION_VERSION}.jar"
CONFIG_TEMPLATE="bioid.properties.template"
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

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
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
    
    if [ ! -d "$kc_home/providers" ]; then
        log_warning "Providers directory not found, creating: $kc_home/providers"
        mkdir -p "$kc_home/providers"
    fi
    
    if [ ! -d "$kc_home/conf" ]; then
        log_warning "Configuration directory not found, creating: $kc_home/conf"
        mkdir -p "$kc_home/conf"
    fi
    
    return 0
}

# Function to backup existing installation
backup_existing() {
    local kc_home="$1"
    local backup_dir="$kc_home/backup/bioid-extension-$(date +%Y%m%d-%H%M%S)"
    
    if [ -f "$kc_home/providers/$EXTENSION_JAR" ] || [ -f "$kc_home/conf/$CONFIG_FILE" ]; then
        log_info "Creating backup of existing installation..."
        mkdir -p "$backup_dir"
        
        if [ -f "$kc_home/providers/$EXTENSION_JAR" ]; then
            cp "$kc_home/providers/$EXTENSION_JAR" "$backup_dir/"
            log_info "Backed up existing JAR to $backup_dir/"
        fi
        
        if [ -f "$kc_home/conf/$CONFIG_FILE" ]; then
            cp "$kc_home/conf/$CONFIG_FILE" "$backup_dir/"
            log_info "Backed up existing configuration to $backup_dir/"
        fi
        
        log_success "Backup created at $backup_dir"
    fi
}

# Function to install extension JAR
install_jar() {
    local kc_home="$1"
    local source_jar="lib/$EXTENSION_JAR"
    local target_jar="$kc_home/providers/$EXTENSION_JAR"
    
    if [ ! -f "$source_jar" ]; then
        log_error "Extension JAR not found: $source_jar"
        return 1
    fi
    
    log_info "Installing extension JAR..."
    cp "$source_jar" "$target_jar"
    
    if [ -f "$target_jar" ]; then
        log_success "Extension JAR installed: $target_jar"
        return 0
    else
        log_error "Failed to install extension JAR"
        return 1
    fi
}

# Function to install configuration
install_config() {
    local kc_home="$1"
    local source_config="config/$CONFIG_TEMPLATE"
    local target_config="$kc_home/conf/$CONFIG_FILE"
    
    if [ ! -f "$source_config" ]; then
        log_error "Configuration template not found: $source_config"
        return 1
    fi
    
    if [ -f "$target_config" ]; then
        log_warning "Configuration file already exists: $target_config"
        read -p "Do you want to overwrite it? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "Skipping configuration installation"
            return 0
        fi
    fi
    
    log_info "Installing configuration template..."
    cp "$source_config" "$target_config"
    
    if [ -f "$target_config" ]; then
        log_success "Configuration template installed: $target_config"
        log_warning "Please edit $target_config with your BioID credentials"
        return 0
    else
        log_error "Failed to install configuration template"
        return 1
    fi
}

# Function to set file permissions
set_permissions() {
    local kc_home="$1"
    
    log_info "Setting file permissions..."
    
    # Set JAR permissions
    if [ -f "$kc_home/providers/$EXTENSION_JAR" ]; then
        chmod 644 "$kc_home/providers/$EXTENSION_JAR"
    fi
    
    # Set config permissions (more restrictive due to sensitive data)
    if [ -f "$kc_home/conf/$CONFIG_FILE" ]; then
        chmod 600 "$kc_home/conf/$CONFIG_FILE"
    fi
    
    log_success "File permissions set"
}

# Function to verify installation
verify_installation() {
    local kc_home="$1"
    local jar_path="$kc_home/providers/$EXTENSION_JAR"
    local config_path="$kc_home/conf/$CONFIG_FILE"
    
    log_info "Verifying installation..."
    
    local errors=0
    
    if [ ! -f "$jar_path" ]; then
        log_error "Extension JAR not found: $jar_path"
        ((errors++))
    else
        log_success "Extension JAR verified: $jar_path"
    fi
    
    if [ ! -f "$config_path" ]; then
        log_error "Configuration file not found: $config_path"
        ((errors++))
    else
        log_success "Configuration file verified: $config_path"
    fi
    
    if [ $errors -eq 0 ]; then
        log_success "Installation verification completed successfully"
        return 0
    else
        log_error "Installation verification failed with $errors errors"
        return 1
    fi
}

# Function to display post-installation instructions
show_post_install_instructions() {
    local kc_home="$1"
    
    echo
    log_success "Installation completed successfully!"
    echo
    echo "Next steps:"
    echo "1. Edit the configuration file: $kc_home/conf/$CONFIG_FILE"
    echo "   - Set your BioID BWS Client ID and Key"
    echo "   - Configure regional endpoint if needed"
    echo "   - Adjust other settings as required"
    echo
    echo "2. Restart Keycloak to load the extension:"
    echo "   $kc_home/bin/kc.sh start"
    echo
    echo "3. Configure face authentication in the Keycloak Admin Console:"
    echo "   - Navigate to Authentication → Flows"
    echo "   - Add Face Authenticator to your authentication flow"
    echo "   - Enable Face Enrollment required action"
    echo
    echo "4. Verify the installation by checking Keycloak logs for BioID extension messages"
    echo
    echo "For detailed configuration and troubleshooting, see docs/INSTALLATION.md"
}

# Main installation function
main() {
    echo "Keycloak BioID Face Recognition Extension Installer"
    echo "Version: $EXTENSION_VERSION"
    echo "=================================================="
    echo
    
    # Check prerequisites
    log_info "Checking prerequisites..."
    
    if ! command_exists java; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 21 ]; then
        log_error "Java 21 or higher is required (found: $java_version)"
        exit 1
    fi
    
    log_success "Java $java_version detected"
    
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
    
    # Confirm installation
    echo
    read -p "Install BioID extension to $keycloak_home? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Installation cancelled"
        exit 0
    fi
    
    # Backup existing installation
    backup_existing "$keycloak_home"
    
    # Install components
    if ! install_jar "$keycloak_home"; then
        exit 1
    fi
    
    if ! install_config "$keycloak_home"; then
        exit 1
    fi
    
    # Set permissions
    set_permissions "$keycloak_home"
    
    # Verify installation
    if ! verify_installation "$keycloak_home"; then
        exit 1
    fi
    
    # Show post-installation instructions
    show_post_install_instructions "$keycloak_home"
}

# Run main function
main "$@"