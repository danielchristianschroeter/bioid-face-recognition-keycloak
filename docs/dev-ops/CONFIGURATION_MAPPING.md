# Configuration Variable Mapping

This document shows the mapping between environment variables (`.env`) and Java properties (`bioid.properties`).

## Variable Mapping Table

| Environment Variable | Java Property | Default Value | Description |
|---------------------|---------------|---------------|-------------|
| **BioID Service** |
| `BWS_CLIENT_ID` | `bioid.clientId` | *Required* | BioID BWS Client ID |
| `BWS_KEY` | `bioid.key` | *Required* | BioID BWS Secret Key |
| `BWS_ENDPOINT` | `bioid.endpoint` | `.bws-eu.bioid.com` | BioID service endpoint |
| `BWS_JWT_EXPIRE_MINUTES` | `bioid.jwtExpireMinutes` | `60` | JWT token expiration |
| **Verification** |
| `VERIFICATION_THRESHOLD` | `verification.threshold` | `0.015` | Face verification threshold |
| `VERIFICATION_MAX_RETRIES` | `verification.maxRetries` | `3` | Maximum retry attempts |
| `VERIFICATION_TIMEOUT_SECONDS` | `verification.timeoutSeconds` | `4` | Verification timeout |
| `ENROLLMENT_TIMEOUT_SECONDS` | `enrollment.timeoutSeconds` | `7` | Enrollment timeout |
| **Template Management** |
| `TEMPLATE_TTL_DAYS` | `template.ttl.days` | `730` | Template time-to-live |
| `TEMPLATE_CLEANUP_INTERVAL_HOURS` | `template.cleanupInterval.hours` | `24` | Cleanup interval |
| `TEMPLATE_TYPE` | `template.type` | `STANDARD` | Template type |
| `TEMPLATE_ENCRYPTION_ENABLED` | `template.encryption.enabled` | `true` | Enable encryption |
| **Liveness Detection** |
| `LIVENESS_ACTIVE_ENABLED` | `liveness.active.enabled` | `true` | Enable active liveness |
| `LIVENESS_CHALLENGE_RESPONSE_ENABLED` | `liveness.challengeResponse.enabled` | `false` | Enable challenge-response |
| `LIVENESS_CONFIDENCE_THRESHOLD` | `liveness.confidenceThreshold` | `0.5` | Confidence threshold |
| `LIVENESS_MAX_OVERHEAD_MS` | `liveness.maxOverheadMs` | `200` | Max processing overhead |
| `LIVENESS_ADAPTIVE_MODE` | `liveness.adaptiveMode` | `false` | Enable adaptive mode |
| `LIVENESS_FALLBACK_TO_PASSWORD` | `liveness.fallbackToPassword` | `false` | Allow password fallback |
| `LIVENESS_CHALLENGE_COUNT` | `liveness.challengeCount` | `1` | Number of challenges |
| `LIVENESS_CHALLENGE_TIMEOUT_SECONDS` | `liveness.challengeTimeoutSeconds` | `30` | Challenge timeout |
| **gRPC Connection** |
| `GRPC_CHANNEL_POOL_SIZE` | `grpc.channelPool.size` | `5` | Connection pool size |
| `GRPC_KEEP_ALIVE_TIME_SECONDS` | `grpc.keepAlive.timeSeconds` | `30` | Keep-alive time |
| `GRPC_RETRY_MAX_ATTEMPTS` | `grpc.retry.maxAttempts` | `3` | Max retry attempts |
| `GRPC_RETRY_BACKOFF_MULTIPLIER` | `grpc.retry.backoffMultiplier` | `2.0` | Retry backoff multiplier |
| **Health Checks** |
| `HEALTH_CHECK_INTERVAL_SECONDS` | `healthCheck.interval.seconds` | `30` | Health check interval |
| `HEALTH_CHECK_TIMEOUT_SECONDS` | `healthCheck.timeout.seconds` | `5` | Health check timeout |
| **Regional Settings** |
| `REGIONAL_PREFERRED_REGION` | `regional.preferredRegion` | `EU` | Preferred region |
| `REGIONAL_DATA_RESIDENCY_REQUIRED` | `regional.dataResidencyRequired` | `false` | Require data residency |
| `REGIONAL_FAILOVER_ENABLED` | `regional.failoverEnabled` | `true` | Enable failover |
| `REGIONAL_LATENCY_THRESHOLD_MS` | `regional.latencyThresholdMs` | `1000` | Latency threshold |
| **Debug Settings** |
| `DEBUG_IMAGE_STORAGE_ENABLED` | `debug.image.storage.enabled` | `false` | Enable image debug storage |
| `DEBUG_IMAGE_STORAGE_PATH` | `debug.image.storage.path` | `./debug-images` | Debug storage path |
| `DEBUG_IMAGE_STORAGE_INCLUDE_METADATA` | `debug.image.storage.includeMetadata` | `true` | Include metadata files |
| **Keycloak** |
| `KEYCLOAK_ADMIN` | N/A | `admin` | Keycloak admin username |
| `KEYCLOAK_ADMIN_PASSWORD` | N/A | `admin123` | Keycloak admin password |
| **Database** |
| `POSTGRES_DB` | N/A | `keycloak` | PostgreSQL database name |
| `POSTGRES_USER` | N/A | `keycloak` | PostgreSQL username |
| `POSTGRES_PASSWORD` | N/A | `keycloak` | PostgreSQL password |

## Configuration Priority

Configuration values are resolved in the following order (highest to lowest priority):

1. **System Properties** - JVM arguments (`-Dbioid.clientId=...`)
2. **Environment Variables** - OS environment (`BWS_CLIENT_ID=...`)
3. **Properties File** - `${kc.home}/conf/bioid.properties`
4. **Default Values** - Built-in defaults

## Usage Examples

### Using Environment Variables (Docker Compose)

```yaml
# docker-compose.yml
services:
  keycloak:
    environment:
      BWS_CLIENT_ID: your-client-id
      BWS_KEY: your-secret-key
      LIVENESS_ACTIVE_ENABLED: true
```

### Using Properties File

```properties
# ${kc.home}/conf/bioid.properties
bioid.clientId=your-client-id
bioid.key=your-secret-key
liveness.active.enabled=true
```

### Using System Properties

```bash
# Start Keycloak with system properties
kc.sh start \
  -Dbioid.clientId=your-client-id \
  -Dbioid.key=your-secret-key \
  -Dliveness.active.enabled=true
```

## Configuration Files

- **`.env.example`** - Template for Docker Compose environment variables
- **`bioid.properties.template`** - Template for Java properties file
- **`.env`** - Your actual environment configuration (not in git)
- **`${kc.home}/conf/bioid.properties`** - Your actual properties file (not in git)

## Notes

### Environment Variable Naming Convention
- Uppercase with underscores: `BWS_CLIENT_ID`
- Corresponds to property: `bioid.clientId`

### Property Naming Convention
- Lowercase with dots: `bioid.clientId`
- Corresponds to environment: `BWS_CLIENT_ID`

### Boolean Values
- Environment: `true` or `false` (lowercase)
- Properties: `true` or `false` (lowercase)

### Numeric Values
- No quotes needed in either format
- Example: `VERIFICATION_THRESHOLD=0.015` or `verification.threshold=0.015`

### String Values
- No quotes needed in either format
- Example: `BWS_ENDPOINT=.bws-eu.bioid.com` or `bioid.endpoint=.bws-eu.bioid.com`

## Validation

The extension validates configuration on startup and will log warnings for:
- Missing required values (clientId, key)
- Invalid numeric ranges
- Invalid boolean values
- Deprecated configuration options

Check Keycloak logs for validation messages:
```bash
docker compose logs keycloak | grep -i "bioid\|configuration"
```

## Troubleshooting

### Configuration Not Applied

1. **Check configuration priority** - System properties override environment variables
2. **Verify file location** - Properties file must be in `${kc.home}/conf/`
3. **Check syntax** - No spaces around `=` in properties files
4. **Restart Keycloak** - Configuration is loaded on startup

### Environment Variables Not Working

1. **Check Docker Compose** - Ensure variables are in `environment:` section
2. **Verify variable names** - Must be exact (case-sensitive)
3. **Check .env file** - Must be in same directory as docker-compose.yml
4. **Restart containers** - `docker compose restart keycloak`

### Properties File Not Loading

1. **Check file location** - Must be `${kc.home}/conf/bioid.properties`
2. **Check file permissions** - Must be readable by Keycloak process
3. **Check syntax** - Use `key=value` format, no spaces around `=`
4. **Check logs** - Look for "Loading configuration from" messages

## Best Practices

1. **Use .env for Docker** - Easier to manage in containerized environments
2. **Use properties for production** - More secure, not in environment
3. **Never commit secrets** - Add `.env` and `bioid.properties` to `.gitignore`
4. **Use templates** - Copy from `.env.example` or `bioid.properties.template`
5. **Document changes** - Update this mapping when adding new configuration
6. **Validate on startup** - Check logs for configuration warnings
7. **Use defaults wisely** - Only override when necessary

## Security Considerations

### Sensitive Values
These values should never be committed to version control:
- `BWS_CLIENT_ID` / `bioid.clientId`
- `BWS_KEY` / `bioid.key`
- `KEYCLOAK_ADMIN_PASSWORD`
- `POSTGRES_PASSWORD`

### Secure Storage
For production:
- Use secrets management (Vault, AWS Secrets Manager, etc.)
- Use environment variables from secure sources
- Restrict file permissions on properties files
- Use encrypted configuration where possible

### Audit Trail
Configuration changes should be:
- Logged and monitored
- Reviewed in security audits
- Documented in change management
- Tested in non-production first
