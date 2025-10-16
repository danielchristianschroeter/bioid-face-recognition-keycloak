# Windows Development Setup Guide

This guide provides Windows-specific instructions for setting up the Keycloak BioID Face Recognition Extension.

## Prerequisites for Windows

### Required Software

1. **Java Development Kit 21+**
   - Download from [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
   - Verify installation: `java -version`

2. **Apache Maven 3.8+**
   - Download from [Maven Downloads](https://maven.apache.org/download.cgi)
   - Add to PATH environment variable
   - Verify installation: `mvn -version`

3. **Docker Desktop for Windows**
   - Download from [Docker Desktop](https://www.docker.com/products/docker-desktop/)
   - Enable WSL 2 backend (recommended)
   - Verify installation: `docker --version`

4. **Git for Windows**
   - Download from [Git for Windows](https://gitforwindows.org/)
   - Includes Git Bash terminal

5. **BioID BWS Account**
   - Register at [BioID BWS Portal](https://bwsportal.bioid.com/register)

### Recommended Tools

- **Windows Terminal** - Modern terminal with tabs and better PowerShell support
- **Visual Studio Code** - IDE with Java and Docker extensions
- **IntelliJ IDEA** - Full-featured Java IDE

## Quick Setup (PowerShell)

Open PowerShell as Administrator and run:

```powershell
# Clone the repository
git clone <repository-url>
cd bioid-face-recognition-keycloak

# Build the extension
mvn clean package

# Copy environment template
Copy-Item .env.example .env

# Edit .env file (opens in notepad)
notepad .env
```

## Step-by-Step Setup

### 1. Environment Setup

Create your `.env` file with your BioID credentials:

```bash
# Required BioID Configuration
BWS_CLIENT_ID=your_actual_client_id_here
BWS_KEY=your_actual_secret_key_here

# Optional Configuration
BWS_ENDPOINT=face.bws-eu.bioid.com
BIOID_PREFERRED_REGION=EU
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
```

### 2. Build the Extension

Using Command Prompt or PowerShell:

```cmd
# Clean build
mvn clean package

# Skip tests if you don't have BioID credentials configured
mvn clean package -DskipTests

# Verify the JAR was created
dir deployment\target\keycloak-bioid-extension-*.jar
```

### 3. Start Development Environment

```cmd
# Start all services
docker compose up -d

# View logs
docker compose logs -f keycloak

# Check service status
docker compose ps
```

### 4. Test the Setup

Run the Windows test script:

```cmd
# Make sure you're in the project root directory
scripts\test-setup.bat
```

Or test manually:

```cmd
# Test Keycloak health
curl http://localhost:8080/health

# Test admin console (should return HTML)
curl http://localhost:8080/admin/

# Check if services are running
docker compose ps
```

## Windows-Specific Commands

### PowerShell Commands

```powershell
# Build and restart Keycloak
mvn clean package; docker compose restart keycloak

# View real-time logs
docker compose logs -f keycloak

# Stop all services
docker compose down

# Clean up everything including volumes
docker compose down -v
docker system prune -f
```

### Command Prompt Commands

```cmd
REM Build and restart
mvn clean package && docker compose restart keycloak

REM View logs
docker compose logs keycloak

REM Stop services
docker compose down
```

## Development Workflow on Windows

### 1. Code Changes

```cmd
# Make your code changes in your IDE
# Then rebuild and restart:

mvn clean package -DskipTests
docker compose restart keycloak
```

### 2. View Logs

```cmd
# Follow logs in real-time
docker compose logs -f keycloak

# View specific number of log lines
docker compose logs --tail=100 keycloak

# Search logs for errors
docker compose logs keycloak | findstr ERROR
```

### 3. Database Access

```cmd
# Connect to PostgreSQL
docker compose exec postgres psql -U keycloak -d keycloak

# Backup database
docker compose exec postgres pg_dump -U keycloak keycloak > backup.sql

# Restore database
docker compose exec -T postgres psql -U keycloak keycloak < backup.sql
```

## Windows-Specific Troubleshooting

### Common Issues

#### 1. Docker Desktop Issues

**Problem**: Docker commands fail or containers won't start

**Solutions**:
```cmd
# Restart Docker Desktop
# Check Docker Desktop settings
# Enable WSL 2 integration
# Increase memory allocation in Docker Desktop settings
```

#### 2. Path Issues

**Problem**: Maven or Java commands not found

**Solutions**:
```cmd
# Check PATH environment variable
echo %PATH%

# Add to PATH via System Properties > Environment Variables
# Or use full paths:
"C:\Program Files\Java\jdk-21\bin\java" -version
"C:\Program Files\Apache\maven\bin\mvn" -version
```

#### 3. Port Conflicts

**Problem**: Port 8080 already in use

**Solutions**:
```cmd
# Find what's using the port
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F

# Or change ports in docker compose.yml
```

#### 4. File Permission Issues

**Problem**: Cannot write to mounted volumes

**Solutions**:
- Run Docker Desktop as Administrator
- Check Docker Desktop file sharing settings
- Ensure the project directory is in a shared drive

#### 5. Line Ending Issues

**Problem**: Shell scripts fail due to Windows line endings

**Solutions**:
```cmd
# Configure Git to handle line endings
git config --global core.autocrlf true

# Or use Git Bash for shell commands
```

### Performance Tips for Windows

1. **Use WSL 2**: Better performance than Hyper-V
2. **Exclude from Windows Defender**: Add project directory to exclusions
3. **Use SSD**: Store project on SSD for better I/O performance
4. **Increase Docker Memory**: Allocate more RAM to Docker Desktop

## IDE Setup

### Visual Studio Code

Install these extensions:
- Extension Pack for Java
- Docker
- Maven for Java
- Spring Boot Extension Pack

### IntelliJ IDEA

1. Import as Maven project
2. Set Project SDK to Java 21
3. Enable Docker plugin
4. Configure Maven settings

## Testing on Windows

### Unit Tests

```cmd
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BioIdConfigurationTest

# Run tests with specific profile
mvn test -Pwindows
```

### Integration Tests

```cmd
# Run with real BioID credentials (set in .env)
mvn test -Dtest=*IntegrationTest

# Skip integration tests
mvn test -DskipITs
```

## Deployment from Windows

### Building for Production

```cmd
# Clean production build
mvn clean package -Pprod

# Create distribution
mvn clean package
```

### Docker Build

```cmd
# Build custom Keycloak image
docker build -t keycloak-bioid .

# Tag for registry
docker tag keycloak-bioid your-registry/keycloak-bioid:latest

# Push to registry
docker push your-registry/keycloak-bioid:latest
```

## Windows-Specific Scripts

All scripts are provided in both Unix (`.sh`) and Windows (`.bat`) formats:

- `scripts/test-setup.bat` - Test the Docker setup
- `scripts/build.bat` - Build the extension
- `scripts/deploy.bat` - Deploy to local Keycloak

## Getting Help

### Log Locations

- Docker logs: `docker compose logs keycloak`
- Maven logs: Check console output
- Windows Event Viewer: For system-level issues

### Useful Commands

```cmd
# Check Java installation
java -version
javac -version

# Check Maven installation
mvn -version

# Check Docker installation
docker --version
docker compose --version

# Check network connectivity
ping face.bws-eu.bioid.com
telnet face.bws-eu.bioid.com 443
```

### Support Resources

1. Check Windows-specific issues in the main [SETUP.md](SETUP.md)
2. Review Docker Desktop documentation
3. Check Windows Subsystem for Linux (WSL) if using WSL 2
4. Verify Windows Defender exclusions

## Next Steps

1. Follow the main [SETUP.md](SETUP.md) for Keycloak configuration
2. Set up your IDE for Java development
3. Configure face authentication in Keycloak
4. Test with real users and face enrollment

Remember to keep your BioID credentials secure and never commit them to version control!