# Keycloak BioID Face Recognition Extension - Installation Guide

## Overview

The Keycloak BioID Face Recognition Extension provides face-based authentication capabilities for Keycloak using the BioID BWS 3 gRPC service. This guide covers installation, configuration, and deployment procedures.

## Prerequisites

- Keycloak ${keycloak.version} or compatible version
- Java 21 or higher
- BioID BWS account and credentials from https://bwsportal.bioid.com/register
- Network access to BioID BWS endpoints

## Installation Steps

### 1. Download and Extract

Extract the distribution archive to a temporary directory:

```bash
unzip keycloak-bioid-extension-${project.version}.zip
cd keycloak-bioid-extension-${project.version}
```

### 2. Deploy Extension JAR

Copy the extension JAR to your Keycloak providers directory:

```bash
# For Keycloak in standalone mode
cp lib/keycloak-bioid-extension-${project.version}.jar $KEYCLOAK_HOME/providers/

# For Keycloak in container/cloud deployment
# Copy to the appropriate providers directory based on your deployment method
```

### 3. Configure BioID Settings

Copy and configure the BioID properties file:

```bash
# Copy template to Keycloak configuration directory
cp config/bioid.properties.template $KEYCLOAK_HOME/conf/bioid.properties

# Edit the configuration file
vi $KEYCLOAK_HOME/conf/bioid.properties
```

Required configuration parameters:

```properties
# BioID BWS Service Configuration (Required)
bioid.clientId=YOUR_BWS_CLIENT_ID
bioid.key=YOUR_BWS_KEY
bioid.endpoint=grpcs://face.bws-eu.bioid.com:443

# Optional: Adjust other settings as needed
verification.threshold=0.015
verification.maxRetries=3
liveness.enabled=true
```

### 4. Restart Keycloak

Restart your Keycloak instance to load the extension:

```bash
# For standalone Keycloak
$KEYCLOAK_HOME/bin/kc.sh start

# For development mode
$KEYCLOAK_HOME/bin/kc.sh start-dev

# For containerized deployments, restart the container
```

### 5. Verify Installation

Check the Keycloak logs for successful extension loading:

```bash
tail -f $KEYCLOAK_HOME/data/log/keycloak.log
```

Look for log entries indicating successful BioID extension initialization.

## Configuration Options

### Environment Variables

You can configure the extension using environment variables instead of the properties file:

```bash
export BWS_CLIENT_ID="your_client_id"
export BWS_KEY="your_secret_key"
export BWS_ENDPOINT="grpcs://face.bws-eu.bioid.com:443"
export VERIFICATION_THRESHOLD="0.015"
export LIVENESS_ENABLED="true"
```

### System Properties

Alternatively, use Java system properties:

```bash
-Dbioid.clientId=your_client_id
-Dbioid.key=your_secret_key
-Dbioid.endpoint=grpcs://face.bws-eu.bioid.com:443
```

### Regional Endpoints

Configure regional endpoints for optimal performance and data residency:

```properties
# Regional settings
regional.preferredRegion=EU
regional.dataResidencyRequired=false
regional.failoverEnabled=true

# Available regions:
# EU: grpcs://face.bws-eu.bioid.com:443
# US: grpcs://face.bws-us.bioid.com:443
# SA: grpcs://face.bws-sa.bioid.com:443
```

### Liveness Detection

Configure liveness detection settings:

```properties
# Liveness detection
liveness.enabled=true
liveness.passive.enabled=true
liveness.active.enabled=false
liveness.challengeResponse.enabled=false
liveness.confidenceThreshold=0.5
liveness.maxOverheadMs=200
```

## Realm Configuration

### 1. Enable Face Authentication

1. Log in to the Keycloak Admin Console
2. Select your realm
3. Navigate to **Authentication** → **Flows**
4. Create a new authentication flow or modify an existing one
5. Add the **Face Authenticator** execution
6. Configure the execution as required or alternative

### 2. Configure Face Enrollment

1. Navigate to **Authentication** → **Required Actions**
2. Enable the **Face Enrollment** required action
3. Set as default action for new users if desired

### 3. Configure Face Credentials

The Face Credential Provider is automatically registered and available for user credential management.

## User Experience

### Face Enrollment Process

1. Users will be prompted to enroll their face during first login or when the required action is triggered
2. The enrollment process captures multiple face images with different angles
3. Users receive real-time feedback during the capture process
4. Enrollment completion is confirmed with success message

### Face Authentication Process

1. Users are presented with a face authentication interface
2. Single image capture for verification
3. Optional liveness detection based on configuration
4. Fallback to password authentication if face authentication fails

## Troubleshooting

### Common Issues

#### Extension Not Loading

- Verify JAR file is in the correct providers directory
- Check Keycloak logs for error messages
- Ensure Java 21 compatibility

#### BioID Connection Issues

- Verify network connectivity to BioID endpoints
- Check BioID credentials are correct
- Review firewall and proxy settings
- Test with different regional endpoints

#### Face Authentication Failures

- Check verification threshold settings
- Review image quality requirements
- Verify liveness detection configuration
- Check user enrollment status

### Log Configuration

Enable debug logging for troubleshooting:

```xml
<!-- Add to Keycloak logging configuration -->
<logger category="com.bioid.keycloak" level="DEBUG"/>
```

### Health Checks

Monitor extension health through:

- Keycloak admin console
- Application logs
- BioID service connectivity
- Face authentication success rates

## Security Considerations

### Network Security

- Use TLS/SSL for all BioID communications
- Configure appropriate firewall rules
- Consider VPN for sensitive deployments

### Data Protection

- Face templates are stored encrypted
- No raw biometric data is persisted
- Automatic cleanup of expired templates
- GDPR compliance features available

### Access Control

- Limit admin access to BioID configuration
- Monitor face authentication events
- Implement appropriate audit logging

## Performance Optimization

### Connection Pooling

```properties
# gRPC connection settings
grpc.channelPool.size=5
grpc.keepAlive.timeSeconds=30
grpc.retry.maxAttempts=3
```

### Regional Optimization

- Choose nearest regional endpoint
- Enable failover for high availability
- Monitor latency and adjust settings

### Template Management

```properties
# Template lifecycle
template.ttl.days=730
template.cleanupInterval.hours=24
```

## Monitoring and Metrics

### Available Metrics

- Face enrollment success/failure rates
- Face authentication success/failure rates
- BioID service response times
- Connection pool utilization
- Template storage statistics

### Integration

The extension supports MicroProfile Metrics for monitoring integration with enterprise monitoring solutions.

## Support and Documentation

### Additional Resources

- [BioID BWS Documentation](https://developer.bioid.com/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- Extension source code and issues: [GitHub Repository]

### Getting Help

1. Check this documentation and troubleshooting section
2. Review Keycloak and extension logs
3. Consult BioID BWS documentation
4. Contact support through appropriate channels

## Version Information

- Extension Version: ${project.version}
- Keycloak Version: ${keycloak.version}
- Java Version: 21+
- Build Date: ${maven.build.timestamp}