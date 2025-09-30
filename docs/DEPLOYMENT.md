# Deployment Guide

This guide covers production deployment of the Keycloak BioID Face Recognition Extension.

## Production Deployment

### Prerequisites

- Keycloak 24.0 or higher
- Java 21 runtime environment
- BioID BWS 3 account and credentials
- Database (PostgreSQL recommended)
- Load balancer (for high availability)
- Monitoring infrastructure

### Deployment Steps

#### 1. Build the Extension

```bash
# Clone the repository
git clone <repository-url>
cd keycloak-bioid-extension

# Build the extension
mvn clean package

# The built JAR will be at:
# deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar
```

#### 2. Deploy to Keycloak

**Single Instance Deployment:**

```bash
# Copy extension to Keycloak providers directory
cp deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar \
   /opt/keycloak/providers/

# Set appropriate permissions
chown keycloak:keycloak /opt/keycloak/providers/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar
chmod 644 /opt/keycloak/providers/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar
```

**Cluster Deployment:**

Deploy the extension to all Keycloak nodes:

```bash
# Script to deploy to all nodes
for node in keycloak-node-1 keycloak-node-2 keycloak-node-3; do
    scp deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar \
        $node:/opt/keycloak/providers/
done
```

#### 3. Configure BioID Settings

Create `/opt/keycloak/conf/bioid.properties`:

```properties
# Production BioID Configuration
bioid.clientId=${BWS_CLIENT_ID}
bioid.key=${BWS_KEY}
bioid.endpoint=${BWS_ENDPOINT}

# Production Verification Settings
bioid.verificationThreshold=0.85
bioid.maxRetries=3

# Liveness Detection (recommended for production)
bioid.livenessEnabled=true
bioid.livenessPassiveEnabled=true
bioid.livenessActiveEnabled=true
bioid.livenessChallengeResponseEnabled=false
bioid.livenessConfidenceThreshold=0.75
bioid.livenessMaxOverheadMs=300

# Regional Configuration for Data Residency
bioid.preferredRegion=EU
bioid.dataResidencyRequired=true
bioid.failoverEnabled=true

# Production Connection Settings
bioid.channelPoolSize=10
bioid.keepAliveTimeSeconds=60
bioid.verificationTimeoutSeconds=15
bioid.enrollmentTimeoutSeconds=45

# Template Management
bioid.templateTtlDays=1095  # 3 years
```

#### 4. Environment Variables

Set environment variables for sensitive configuration:

```bash
# /etc/environment or systemd service file
BWS_CLIENT_ID=your-production-client-id
BWS_KEY=your-production-key
BWS_ENDPOINT=face.bws-eu.bioid.com
```

#### 5. Database Configuration

Ensure your database is properly configured for the additional tables:

```sql
-- The extension will automatically create these tables
-- Verify they exist after first startup:

-- Face credentials table
SELECT * FROM information_schema.tables WHERE table_name = 'FACE_CREDENTIAL';

-- Deletion requests table  
SELECT * FROM information_schema.tables WHERE table_name = 'DELETION_REQUEST';
```

#### 6. Start Keycloak

```bash
# Production startup command
/opt/keycloak/bin/kc.sh start \
  --hostname=your-keycloak-domain.com \
  --https-certificate-file=/path/to/cert.pem \
  --https-certificate-key-file=/path/to/key.pem \
  --db=postgres \
  --db-url=jdbc:postgresql://db-host:5432/keycloak \
  --db-username=keycloak \
  --db-password=${DB_PASSWORD}
```

## High Availability Deployment

### Load Balancer Configuration

Configure your load balancer for sticky sessions:

**Nginx Example:**

```nginx
upstream keycloak {
    ip_hash;  # Sticky sessions
    server keycloak-node-1:8080;
    server keycloak-node-2:8080;
    server keycloak-node-3:8080;
}

server {
    listen 443 ssl;
    server_name auth.yourdomain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://keycloak;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Health check endpoint
    location /health {
        proxy_pass http://keycloak/health;
        access_log off;
    }
}
```

### Database Clustering

Use PostgreSQL with replication for high availability:

```yaml
# docker compose.prod.yml example
version: '3.8'
services:
  postgres-primary:
    image: postgres:15
    environment:
      POSTGRES_REPLICATION_MODE: master
      POSTGRES_REPLICATION_USER: replicator
      POSTGRES_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    volumes:
      - postgres_primary_data:/var/lib/postgresql/data

  postgres-replica:
    image: postgres:15
    environment:
      POSTGRES_REPLICATION_MODE: slave
      POSTGRES_REPLICATION_USER: replicator
      POSTGRES_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
      POSTGRES_MASTER_SERVICE: postgres-primary
    volumes:
      - postgres_replica_data:/var/lib/postgresql/data
```

## Monitoring and Observability

### Health Checks

Configure health check endpoints:

```bash
# Kubernetes liveness probe
livenessProbe:
  httpGet:
    path: /health/live
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30

# Kubernetes readiness probe
readinessProbe:
  httpGet:
    path: /health/ready
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Metrics Collection

**Prometheus Configuration:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'keycloak-face-recognition'
    static_configs:
      - targets: ['keycloak:8080']
    metrics_path: '/admin/realms/master/face-recognition/metrics'
    scrape_interval: 30s
    basic_auth:
      username: 'monitoring-user'
      password: 'monitoring-password'
```

**Key Metrics to Monitor:**

- `face_recognition_enroll_success_total`
- `face_recognition_verify_success_total`
- `face_recognition_bioid_latency_ms`
- `face_recognition_health_check_success_total`
- `face_recognition_deletion_request_pending_total`

### Logging Configuration

**Production Logging:**

```properties
# keycloak.conf
log-level=INFO
log-console-format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n

# Face recognition specific logging
logger.com.bioid.keycloak.level=INFO
logger.com.bioid.keycloak.client.level=WARN
logger.com.bioid.keycloak.admin.level=INFO
```

**Log Aggregation:**

```yaml
# filebeat.yml for ELK stack
filebeat.inputs:
- type: log
  paths:
    - /opt/keycloak/data/log/*.log
  fields:
    service: keycloak-face-recognition
    environment: production
```

## Security Configuration

### TLS Configuration

**Certificate Management:**

```bash
# Generate certificate (production should use CA-signed certificates)
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Set appropriate permissions
chmod 600 key.pem
chmod 644 cert.pem
chown keycloak:keycloak key.pem cert.pem
```

**Keycloak TLS Configuration:**

```bash
/opt/keycloak/bin/kc.sh start \
  --hostname-strict=true \
  --hostname-strict-https=true \
  --https-certificate-file=/opt/keycloak/conf/cert.pem \
  --https-certificate-key-file=/opt/keycloak/conf/key.pem
```

### Network Security

**Firewall Rules:**

```bash
# Allow HTTPS traffic
ufw allow 443/tcp

# Allow health checks from load balancer
ufw allow from 10.0.1.0/24 to any port 8080

# Allow database connections
ufw allow from 10.0.2.0/24 to any port 5432

# Block direct HTTP access
ufw deny 8080/tcp
```

### Access Control

**Admin User Configuration:**

```bash
# Create dedicated admin user for face recognition management
/opt/keycloak/bin/kcadm.sh create users \
  -r master \
  -s username=face-admin \
  -s enabled=true

# Assign realm management role
/opt/keycloak/bin/kcadm.sh add-roles \
  -r master \
  --uusername face-admin \
  --rolename realm-admin
```

## Backup and Recovery

### Database Backup

```bash
#!/bin/bash
# backup-keycloak.sh

BACKUP_DIR="/opt/backups/keycloak"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup database
pg_dump -h postgres-host -U keycloak keycloak > $BACKUP_DIR/keycloak_$DATE.sql

# Backup configuration
tar -czf $BACKUP_DIR/config_$DATE.tar.gz /opt/keycloak/conf/

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "*.sql" -mtime +30 -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +30 -delete
```

### Disaster Recovery

**Recovery Procedure:**

1. **Restore Database:**
   ```bash
   psql -h postgres-host -U keycloak keycloak < keycloak_backup.sql
   ```

2. **Restore Configuration:**
   ```bash
   tar -xzf config_backup.tar.gz -C /
   ```

3. **Redeploy Extension:**
   ```bash
   cp keycloak-bioid-extension.jar /opt/keycloak/providers/
   ```

4. **Restart Services:**
   ```bash
   systemctl restart keycloak
   ```

## Performance Tuning

### JVM Configuration

```bash
# /opt/keycloak/conf/keycloak.conf
# JVM heap settings
JAVA_OPTS="-Xms2g -Xmx4g"

# Garbage collection
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# JVM monitoring
JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
```

### Connection Pool Tuning

```properties
# bioid.properties
# Adjust based on expected load
bioid.channelPoolSize=20
bioid.keepAliveTimeSeconds=120
bioid.verificationTimeoutSeconds=20
bioid.enrollmentTimeoutSeconds=60
```

### Database Optimization

```sql
-- Index optimization for face credentials
CREATE INDEX CONCURRENTLY idx_face_credential_user_id ON FACE_CREDENTIAL(USER_ID);
CREATE INDEX CONCURRENTLY idx_face_credential_created_at ON FACE_CREDENTIAL(CREATED_AT);

-- Index optimization for deletion requests
CREATE INDEX CONCURRENTLY idx_deletion_request_status ON DELETION_REQUEST(STATUS);
CREATE INDEX CONCURRENTLY idx_deletion_request_priority ON DELETION_REQUEST(PRIORITY);
CREATE INDEX CONCURRENTLY idx_deletion_request_created_at ON DELETION_REQUEST(CREATED_AT);
```

## Troubleshooting Production Issues

### Common Issues

**Extension Not Loading:**
```bash
# Check Keycloak logs
tail -f /opt/keycloak/data/log/keycloak.log | grep -i "bioid\|face"

# Verify JAR file
ls -la /opt/keycloak/providers/keycloak-bioid-extension*

# Check SPI registration
jar -tf /opt/keycloak/providers/keycloak-bioid-extension*.jar | grep META-INF/services
```

**BioID Connectivity Issues:**
```bash
# Test network connectivity
curl -v https://face.bws-eu.bioid.com

# Check DNS resolution
nslookup face.bws-eu.bioid.com

# Test from Keycloak admin console
# Navigate to Realm Settings > Face Recognition > Test Connectivity
```

**Performance Issues:**
```bash
# Monitor JVM metrics
jstat -gc -t $(pgrep java) 5s

# Check connection pool metrics
curl -s http://localhost:8080/admin/realms/master/face-recognition/metrics | grep pool

# Monitor database connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'keycloak';
```

### Emergency Procedures

**Disable Face Recognition:**
```bash
# Temporarily disable by removing from authentication flow
/opt/keycloak/bin/kcadm.sh get authentication/flows/browser -r your-realm

# Or remove the extension JAR (requires restart)
mv /opt/keycloak/providers/keycloak-bioid-extension*.jar /tmp/
systemctl restart keycloak
```

**Rollback Deployment:**
```bash
# Stop Keycloak
systemctl stop keycloak

# Restore previous version
cp /opt/backups/keycloak-bioid-extension-previous.jar /opt/keycloak/providers/

# Restore configuration
cp /opt/backups/bioid.properties.backup /opt/keycloak/conf/bioid.properties

# Start Keycloak
systemctl start keycloak
```

## Maintenance

### Regular Maintenance Tasks

**Weekly:**
- Review deletion request queue
- Check health check status
- Monitor metrics for anomalies
- Verify backup completion

**Monthly:**
- Update BioID credentials if needed
- Review and clean up old templates
- Analyze performance metrics
- Update documentation

**Quarterly:**
- Security audit and updates
- Performance optimization review
- Disaster recovery testing
- Capacity planning review

### Upgrade Procedures

**Extension Updates:**
1. Test new version in staging environment
2. Schedule maintenance window
3. Backup current configuration and database
4. Deploy new extension version
5. Restart Keycloak services
6. Verify functionality
7. Monitor for issues

**Keycloak Updates:**
1. Review compatibility matrix
2. Test extension with new Keycloak version
3. Update deployment scripts
4. Follow standard Keycloak upgrade procedures
5. Verify extension functionality post-upgrade