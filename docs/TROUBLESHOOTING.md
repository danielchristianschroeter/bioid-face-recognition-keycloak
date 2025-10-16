# Troubleshooting Guide

This guide helps diagnose and resolve common issues with the Keycloak BioID Face Recognition Extension.

## Quick Diagnostic Checklist

Before diving into specific issues, run through this quick checklist:

1. **Extension Loading**
   - [ ] JAR file is in `/opt/keycloak/providers/` directory
   - [ ] Keycloak has been restarted after deployment
   - [ ] No errors in Keycloak startup logs

2. **Configuration**
   - [ ] BioID credentials are correctly configured
   - [ ] Configuration file syntax is valid
   - [ ] Environment variables are properly set

3. **Network Connectivity**
   - [ ] Can reach BioID endpoints from Keycloak server
   - [ ] Firewall allows outbound HTTPS connections
   - [ ] DNS resolution works for BioID endpoints

4. **Authentication Flow**
   - [ ] Face authentication is enabled in the flow
   - [ ] Users have enrolled face credentials
   - [ ] Browser supports camera access

## Common Issues and Solutions

### Extension Loading Issues

#### Issue: Extension Not Found in Admin Console

**Symptoms:**
- Face Recognition tab missing from Realm Settings
- No face authentication option in flows
- Extension-related endpoints return 404

**Diagnosis:**
```bash
# Check if extension JAR is present
ls -la /opt/keycloak/providers/keycloak-bioid-extension*

# Check Keycloak startup logs
grep -i "bioid\|face" /opt/keycloak/data/log/keycloak.log

# Verify SPI registration
jar -tf /opt/keycloak/providers/keycloak-bioid-extension*.jar | grep META-INF/services
```

**Solutions:**
1. **Missing JAR File:**
   ```bash
   # Copy extension to providers directory
   cp keycloak-bioid-extension-1.0.0-SNAPSHOT.jar /opt/keycloak/providers/
   
   # Set correct permissions
   chown keycloak:keycloak /opt/keycloak/providers/keycloak-bioid-extension*
   chmod 644 /opt/keycloak/providers/keycloak-bioid-extension*
   
   # Restart Keycloak
   systemctl restart keycloak
   ```

2. **Incorrect File Permissions:**
   ```bash
   chmod 644 /opt/keycloak/providers/keycloak-bioid-extension*
   chown keycloak:keycloak /opt/keycloak/providers/keycloak-bioid-extension*
   ```

3. **Class Loading Conflicts:**
   ```bash
   # Check for duplicate JARs
   find /opt/keycloak -name "*bioid*" -o -name "*face*"
   
   # Remove duplicates and restart
   ```

#### Issue: Extension Fails to Load

**Symptoms:**
- ClassNotFoundException in logs
- NoSuchMethodError during startup
- Extension partially loaded

**Diagnosis:**
```bash
# Check for dependency conflicts
grep -i "exception\|error" /opt/keycloak/data/log/keycloak.log | grep -i "bioid\|face"

# Verify Java version compatibility
java -version
```

**Solutions:**
1. **Dependency Conflicts:**
   - Rebuild extension with correct Keycloak version
   - Check for conflicting libraries in Keycloak

2. **Java Version Mismatch:**
   - Ensure Java 21 or compatible version
   - Rebuild extension with correct Java target

### Configuration Issues

#### Issue: BioID Service Connection Failed

**Symptoms:**
- Connectivity test fails in admin console
- Authentication attempts timeout
- "Service unavailable" errors

**Diagnosis:**
```bash
# Test network connectivity
curl -v https://face.bws-eu.bioid.com

# Check DNS resolution
nslookup face.bws-eu.bioid.com

# Verify configuration
cat /opt/keycloak/conf/bioid.properties

# Check environment variables
env | grep BWS
```

**Solutions:**
1. **Network Connectivity:**
   ```bash
   # Test from Keycloak server
   curl -I https://face.bws-eu.bioid.com
   
   # Check firewall rules
   iptables -L | grep -i drop
   
   # Verify proxy settings if applicable
   echo $https_proxy
   ```

2. **Invalid Credentials:**
   ```bash
   # Verify credentials in BioID portal
   # Update configuration file
   vim /opt/keycloak/conf/bioid.properties
   
   # Restart Keycloak
   systemctl restart keycloak
   ```

3. **Wrong Endpoint:**
   ```properties
   # Correct endpoints:
   # Europe: face.bws-eu.bioid.com
   # US: face.bws-us.bioid.com
   # South America: face.bws-sa.bioid.com
   bioid.endpoint=face.bws-eu.bioid.com
   ```

#### Issue: Configuration Not Applied

**Symptoms:**
- Changes in admin console not saved
- Default values used instead of configured values
- Configuration test shows old values

**Diagnosis:**
```bash
# Check configuration file permissions
ls -la /opt/keycloak/conf/bioid.properties

# Verify configuration syntax
cat /opt/keycloak/conf/bioid.properties | grep -v "^#" | grep "="

# Check for environment variable override
env | grep BIOID
```

**Solutions:**
1. **File Permissions:**
   ```bash
   chmod 644 /opt/keycloak/conf/bioid.properties
   chown keycloak:keycloak /opt/keycloak/conf/bioid.properties
   ```

2. **Configuration Syntax:**
   ```properties
   # Ensure proper format (no spaces around =)
   bioid.clientId=your-client-id
   bioid.key=your-key
   ```

3. **Environment Variable Conflicts:**
   ```bash
   # Check for conflicting environment variables
   unset BIOID_CLIENT_ID  # if causing conflicts
   ```

### Authentication Issues

#### Issue: Face Authentication Not Available

**Symptoms:**
- Face authentication option missing from login
- Users redirected to password authentication
- Authentication flow skips face recognition

**Diagnosis:**
```bash
# Check authentication flow configuration
/opt/keycloak/bin/kcadm.sh get authentication/flows/browser -r your-realm

# Verify face authenticator is registered
grep -i "face" /opt/keycloak/data/log/keycloak.log
```

**Solutions:**
1. **Authentication Flow Configuration:**
   - Add Face Authenticator to browser flow
   - Set appropriate requirement level (ALTERNATIVE/REQUIRED)
   - Ensure flow is bound to realm

2. **Missing Required Action:**
   - Enable Face Enrollment required action
   - Set as default action for new users

#### Issue: Face Enrollment Fails

**Symptoms:**
- Camera access denied
- Image quality errors
- Enrollment process hangs

**Diagnosis:**
```bash
# Check browser console for JavaScript errors
# Verify camera permissions in browser
# Check enrollment logs
grep -i "enroll" /opt/keycloak/data/log/keycloak.log
```

**Solutions:**
1. **Camera Access:**
   - Ensure HTTPS is used (required for camera access)
   - Check browser permissions
   - Verify camera is not in use by other applications

2. **Image Quality:**
   - Ensure good lighting conditions
   - Check camera resolution settings
   - Verify face is clearly visible and centered

3. **Browser Compatibility:**
   - Test with supported browsers (Chrome, Firefox, Safari, Edge)
   - Check for browser extensions blocking camera access

#### Issue: Face Verification Fails

**Symptoms:**
- Authentication always fails despite correct face
- High false negative rate
- Inconsistent verification results

**Diagnosis:**
```bash
# Check verification threshold settings
grep "verificationThreshold" /opt/keycloak/conf/bioid.properties

# Review verification logs
grep -i "verify\|verification" /opt/keycloak/data/log/keycloak.log

# Check BioID service status
curl -s http://localhost:8080/admin/realms/master/face-recognition/health
```

**Solutions:**
1. **Threshold Too High:**
   ```properties
   # Lower verification threshold (0.6-0.8 recommended)
   bioid.verificationThreshold=0.7
   ```

2. **Poor Image Quality:**
   - Ensure consistent lighting conditions
   - Check camera quality and positioning
   - Verify face is clearly visible

3. **Template Issues:**
   - Re-enroll user with better quality images
   - Check template expiration settings

### Performance Issues

#### Issue: Slow Authentication Response

**Symptoms:**
- Authentication takes longer than 5 seconds
- Timeouts during verification
- Poor user experience

**Diagnosis:**
```bash
# Check BioID service latency
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics | grep latency

# Monitor connection pool
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics | grep pool

# Check network latency
ping face.bws-eu.bioid.com
```

**Solutions:**
1. **Network Optimization:**
   ```properties
   # Use closest regional endpoint
   bioid.endpoint=face.bws-eu.bioid.com  # for Europe
   
   # Increase connection pool size
   bioid.channelPoolSize=10
   
   # Optimize timeouts
   bioid.verificationTimeoutSeconds=15
   ```

2. **Connection Pool Tuning:**
   ```properties
   # Increase pool size for high load
   bioid.channelPoolSize=20
   bioid.keepAliveTimeSeconds=60
   ```

#### Issue: High Memory Usage

**Symptoms:**
- OutOfMemoryError in logs
- Keycloak becomes unresponsive
- Frequent garbage collection

**Diagnosis:**
```bash
# Check JVM memory usage
jstat -gc $(pgrep java)

# Monitor heap usage
jmap -histo $(pgrep java) | head -20

# Check for memory leaks
jcmd $(pgrep java) GC.run_finalization
```

**Solutions:**
1. **JVM Tuning:**
   ```bash
   # Increase heap size
   export JAVA_OPTS="-Xms2g -Xmx4g"
   
   # Use G1 garbage collector
   export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"
   ```

2. **Connection Management:**
   ```properties
   # Reduce connection pool size if memory constrained
   bioid.channelPoolSize=5
   
   # Shorter keep-alive time
   bioid.keepAliveTimeSeconds=30
   ```

### Database Issues

#### Issue: Database Connection Errors

**Symptoms:**
- Cannot save face credentials
- Deletion requests not persisting
- Database connection timeouts

**Diagnosis:**
```bash
# Check database connectivity
psql -h db-host -U keycloak -d keycloak -c "SELECT 1;"

# Verify table existence
psql -h db-host -U keycloak -d keycloak -c "\dt" | grep -i face

# Check connection pool
psql -h db-host -U keycloak -d keycloak -c "SELECT count(*) FROM pg_stat_activity;"
```

**Solutions:**
1. **Database Connection:**
   ```bash
   # Test database connectivity
   telnet db-host 5432
   
   # Check database credentials
   # Verify network connectivity
   ```

2. **Missing Tables:**
   ```sql
   -- Tables should be created automatically
   -- If missing, check Keycloak startup logs
   -- Verify database user has CREATE permissions
   ```

### Admin Console Issues

#### Issue: Face Recognition Tab Missing

**Symptoms:**
- No Face Recognition tab in Realm Settings
- Admin endpoints return 404
- Configuration UI not accessible

**Diagnosis:**
```bash
# Check admin resource registration
grep -i "admin" /opt/keycloak/data/log/keycloak.log | grep -i face

# Verify admin permissions
/opt/keycloak/bin/kcadm.sh get realms/master/users/admin-user-id/role-mappings
```

**Solutions:**
1. **Extension Registration:**
   - Verify extension is properly loaded
   - Check SPI registration files
   - Restart Keycloak

2. **Admin Permissions:**
   - Ensure user has realm-admin role
   - Check client role mappings

#### Issue: Configuration Changes Not Saved

**Symptoms:**
- Configuration reverts after save
- Error messages when saving
- Changes not reflected in behavior

**Diagnosis:**
```bash
# Check admin endpoint logs
grep -i "config\|admin" /opt/keycloak/data/log/keycloak.log

# Verify file permissions
ls -la /opt/keycloak/conf/bioid.properties
```

**Solutions:**
1. **File Permissions:**
   ```bash
   chmod 644 /opt/keycloak/conf/bioid.properties
   chown keycloak:keycloak /opt/keycloak/conf/bioid.properties
   ```

2. **Configuration Validation:**
   - Check for invalid values
   - Verify required fields are filled
   - Test connectivity before saving

## Debugging Tools and Commands

### Log Analysis

```bash
# Real-time log monitoring
tail -f /opt/keycloak/data/log/keycloak.log | grep -i "bioid\|face"

# Search for specific errors
grep -i "exception\|error" /opt/keycloak/data/log/keycloak.log | grep -i "bioid\|face"

# Authentication flow debugging
grep -i "authentication" /opt/keycloak/data/log/keycloak.log | grep -i "face"
```

### Health Check Commands

```bash
# Administrative health check (comprehensive)
curl -s http://localhost:8080/admin/realms/master/face-recognition/health

# Keycloak health check
curl -s http://localhost:8080/health

# BioID connectivity test
curl -v https://face.bws-eu.bioid.com

# Component-specific health checks
curl -s http://localhost:8080/admin/realms/master/face-recognition/health | jq '.componentHealth'

# Health check with authentication
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/health
```

### Configuration Verification

```bash
# Check configuration file
cat /opt/keycloak/conf/bioid.properties

# Verify environment variables
env | grep -E "(BWS|BIOID)_"

# Test configuration via admin API
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/config
```

### Performance Monitoring

```bash
# JVM metrics
jstat -gc -t $(pgrep java) 5s

# Administrative metrics (Prometheus format)
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics

# Metrics summary (JSON format)
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics/summary

# Specific metric queries
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics | grep "bioid.admin.liveness"

# Database connections
psql -c "SELECT count(*) FROM pg_stat_activity WHERE datname = 'keycloak';"

# Trace statistics
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/traces/stats

# Log aggregation statistics
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/logs/stats
```

### Monitoring and Observability Issues

#### Issue: Metrics Not Being Collected

**Symptoms:**
- Metrics endpoint returns empty or minimal data
- Prometheus scraping fails
- Dashboard shows no data

**Diagnosis:**
```bash
# Check metrics collector service status
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics/stats

# Verify metrics registration
grep -i "metrics" /opt/keycloak/data/log/keycloak.log

# Check MicroProfile Metrics availability
curl -s http://localhost:8080/metrics
```

**Solutions:**
1. **Metrics Service Not Started:**
   ```bash
   # Check if metrics collector is running
   grep "metrics.*started" /opt/keycloak/data/log/keycloak.log
   
   # Restart Keycloak if needed
   systemctl restart keycloak
   ```

2. **Insufficient Permissions:**
   ```bash
   # Verify admin user has proper roles
   /opt/keycloak/bin/kcadm.sh get users/admin-user-id/role-mappings
   ```

#### Issue: Health Checks Failing

**Symptoms:**
- Health endpoint returns unhealthy status
- Components showing degraded state
- Monitoring alerts firing

**Diagnosis:**
```bash
# Get detailed health status
curl -s http://localhost:8080/admin/realms/master/face-recognition/health | jq '.'

# Check individual component health
curl -s http://localhost:8080/admin/realms/master/face-recognition/health | jq '.componentHealth'

# Review health check logs
grep -i "health.*check" /opt/keycloak/data/log/keycloak.log
```

**Solutions:**
1. **Component-Specific Issues:**
   - Check BioID service connectivity
   - Verify database connections
   - Review connection pool status

2. **Health Check Configuration:**
   ```properties
   # Adjust health check thresholds
   health.response.time.threshold=10s
   health.error.rate.threshold=0.1
   ```

#### Issue: Alerts Not Firing

**Symptoms:**
- No alerts despite issues
- Alert suppression too aggressive
- Missing alert notifications

**Diagnosis:**
```bash
# Check alerting service status
grep -i "alerting.*service" /opt/keycloak/data/log/keycloak.log

# Review alert configuration
cat /opt/keycloak/conf/monitoring.properties | grep alert

# Check alert suppression status
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/alerts/stats
```

**Solutions:**
1. **Alerting Service Configuration:**
   ```properties
   # Lower alert thresholds for testing
   alert.error.rate.threshold=0.01
   alert.suppression.duration=5m
   ```

2. **Clear Alert Suppressions:**
   ```bash
   # Clear all suppressions via admin API
   curl -X DELETE -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/admin/realms/master/face-recognition/alerts/suppressions
   ```

#### Issue: Distributed Tracing Not Working

**Symptoms:**
- No trace data available
- Correlation IDs missing from logs
- Trace context not propagated

**Diagnosis:**
```bash
# Check tracing service status
grep -i "tracing" /opt/keycloak/data/log/keycloak.log

# Verify trace statistics
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/admin/realms/master/face-recognition/traces/stats

# Check for correlation IDs in logs
grep "correlationId" /opt/keycloak/data/log/keycloak.log | head -5
```

**Solutions:**
1. **Enable Tracing:**
   ```properties
   # Enable distributed tracing
   tracing.enabled=true
   tracing.max.trace.duration=1h
   ```

2. **Trace Cleanup:**
   ```bash
   # Clean up expired traces
   curl -X POST -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/admin/realms/master/face-recognition/traces/cleanup
   ```

## Getting Additional Help

### Information to Collect

When seeking help, collect the following information:

1. **Environment Details:**
   - Keycloak version
   - Java version
   - Operating system
   - Extension version

2. **Configuration:**
   - Sanitized configuration file (remove credentials)
   - Environment variables (remove sensitive values)
   - Authentication flow configuration

3. **Logs:**
   - Keycloak startup logs
   - Error logs with timestamps
   - Browser console logs (for UI issues)

4. **Reproduction Steps:**
   - Exact steps to reproduce the issue
   - Expected vs actual behavior
   - Screenshots or videos if applicable

### Log Collection Script

```bash
#!/bin/bash
# collect-debug-info.sh

DEBUG_DIR="/tmp/keycloak-face-debug-$(date +%Y%m%d_%H%M%S)"
mkdir -p $DEBUG_DIR

# System information
uname -a > $DEBUG_DIR/system-info.txt
java -version 2> $DEBUG_DIR/java-version.txt

# Keycloak information
ls -la /opt/keycloak/providers/ > $DEBUG_DIR/providers.txt
ps aux | grep keycloak > $DEBUG_DIR/processes.txt

# Configuration (sanitized)
cp /opt/keycloak/conf/bioid.properties $DEBUG_DIR/
sed -i 's/bioid\.key=.*/bioid.key=***REDACTED***/g' $DEBUG_DIR/bioid.properties
sed -i 's/bioid\.clientId=.*/bioid.clientId=***REDACTED***/g' $DEBUG_DIR/bioid.properties

# Recent logs
tail -1000 /opt/keycloak/data/log/keycloak.log > $DEBUG_DIR/keycloak.log

# Health check
curl -s http://localhost:8080/health > $DEBUG_DIR/health-check.json 2>&1

# Create archive
tar -czf keycloak-face-debug.tar.gz -C /tmp $(basename $DEBUG_DIR)
echo "Debug information collected in: keycloak-face-debug.tar.gz"
```

### Support Channels

1. **Documentation Review:**
   - Check README.md for basic setup
   - Review DEPLOYMENT.md for production issues
   - Consult DEVELOPMENT.md for development problems

2. **Issue Tracking:**
   - Search existing GitHub issues
   - Create new issue with collected information
   - Include debug information archive

3. **Community Support:**
   - Keycloak community forums
   - Stack Overflow with appropriate tags
   - BioID developer community