# BWS Admin API Module

REST API for BWS face template administration in Keycloak.

## Overview

This module provides a secure REST API for administrators to manage face templates across all users. It integrates directly into Keycloak as a Realm Resource Provider.

## Features

- **Statistics Dashboard** - View aggregated template statistics
- **Template Listing** - List all enrolled face templates with user information
- **Template Details** - Get detailed information about specific templates
- **Template Deletion** - Delete individual templates
- **Validation** - Check consistency between Keycloak and BWS
- **Orphaned Template Detection** - Find templates without users
- **Bulk Operations** - Delete multiple orphaned templates at once
- **Audit Logging** - All admin actions are logged
- **Role-Based Access** - Requires `bws-admin` realm role

## API Endpoints

All endpoints are available at: `/realms/{realm}/bws-admin/*`

### GET /stats
Get template statistics.

**Response:**
```json
{
  "totalTemplates": 42,
  "activeUsers": 40,
  "orphanedTemplates": 2,
  "lastEnrollment": "2024-01-15T10:30:00Z"
}
```

### GET /templates
List all enrolled face templates.

**Response:**
```json
[
  {
    "classId": "12345",
    "username": "john.doe",
    "email": "john@example.com",
    "enrolledAt": "2024-01-15T10:30:00Z",
    "keycloakUserExists": true
  }
]
```

### GET /templates/{classId}
Get details for a specific template.

**Response:**
```json
{
  "classId": "12345",
  "username": "john.doe",
  "email": "john@example.com",
  "enrolledAt": "2024-01-15T10:30:00Z",
  "encoderVersion": "9.2",
  "featureVectors": 3,
  "thumbnailsStored": 3,
  "keycloakUserExists": true
}
```

### DELETE /templates/{classId}
Delete a face template.

**Response:**
```json
{
  "success": true,
  "message": "Template deleted successfully",
  "classId": "12345"
}
```

### GET /validate
Validate template consistency.

**Response:**
```json
{
  "isValid": true,
  "orphanedTemplates": [],
  "missingTemplates": [],
  "message": "Validation complete"
}
```

### GET /orphaned
Find orphaned templates.

**Response:**
```json
[
  {
    "classId": "67890",
    "enrolledAt": "2024-01-10T08:20:00Z",
    "keycloakUserExists": false
  }
]
```

### DELETE /orphaned
Delete all orphaned templates.

**Response:**
```json
{
  "success": true,
  "deletedCount": 2,
  "message": "Deleted 2 orphaned template(s)"
}
```

## Security

### Authentication
All endpoints require a valid Bearer token from Keycloak.

### Authorization
All endpoints require the `bws-admin` realm role.

### Audit Logging
All admin actions are logged with:
- User ID
- Action type
- Timestamp
- Additional details

## Architecture

```
┌─────────────────┐
│   test-app.html │
│   (Admin UI)    │
└────────┬────────┘
         │ JWT with bws-admin role
         ↓
┌─────────────────┐
│ BWSAdminResource│
│  (REST Layer)   │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ BWSAdminService │
│ (Business Logic)│
└────────┬────────┘
         │
         ├──→ Keycloak User Management
         │
         └──→ BioID BWS Client
```

## Components

### REST Layer
- `BWSAdminResource` - JAX-RS resource with endpoints
- `BWSAdminResourceProvider` - Keycloak resource provider
- `BWSAdminResourceProviderFactory` - SPI factory

### Service Layer
- `BWSAdminService` - Business logic and coordination

### Data Transfer Objects
- `AdminStats` - Statistics data
- `TemplateInfo` - Template information
- `ValidationResult` - Validation results

## Building

```bash
# Build this module only
mvn clean install

# Build entire project
cd ..
mvn clean install
```

## Deployment

The module is automatically included in the deployment JAR:

```bash
# Build deployment package
mvn clean install

# Copy to Keycloak
cp deployment/target/keycloak-bioid-extension-1.0.0-SNAPSHOT.jar \
   /opt/keycloak/providers/

# Restart Keycloak
docker compose restart keycloak
```

## Testing

### 1. Create bws-admin Role

```bash
# In Keycloak Admin Console
Realm Settings → Roles → Create role
Role name: bws-admin
```

### 2. Assign Role to User

```bash
# In Keycloak Admin Console
Users → Select user → Role mapping → Assign role → bws-admin
```

### 3. Test with curl

```bash
# Get access token
TOKEN=$(curl -X POST "http://localhost:8080/realms/bioid-demo/protocol/openid-connect/token" \
  -d "client_id=bioid-demo-client" \
  -d "username=admin-test" \
  -d "password=password" \
  -d "grant_type=password" \
  | jq -r '.access_token')

# Test stats endpoint
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/realms/bioid-demo/bws-admin/stats

# Test list templates
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/realms/bioid-demo/bws-admin/templates
```

### 4. Test with UI

1. Open http://localhost:3000/test-app.html
2. Login with admin user
3. Admin panel should appear
4. Click buttons to test functionality

## Performance Considerations

### Caching
- User queries are not cached (always fresh data)
- Consider implementing caching for large user bases

### Pagination
- Current implementation loads all templates
- Consider adding pagination for > 1000 users

### Rate Limiting
- Configured via `BWS_ADMIN_RATE_LIMIT_REQUESTS_PER_MINUTE`
- Default: 60 requests per minute

## Monitoring

### Metrics
- Request count per endpoint
- Response times
- Error rates
- Admin action counts

### Logging
- All requests logged at INFO level
- Errors logged at ERROR level
- Audit actions logged separately

## Troubleshooting

### Issue: 403 Forbidden

**Cause:** User doesn't have `bws-admin` role

**Solution:** Assign role in Keycloak Admin Console

### Issue: 500 Internal Server Error

**Cause:** BioID credentials not configured

**Solution:** Check `.env` file has BWS_CLIENT_ID and BWS_KEY

### Issue: Empty template list

**Cause:** No users have enrolled faces

**Solution:** Enroll at least one user via Account Management

## Future Enhancements

### BWS Management API Integration
- List all class IDs from BWS
- Detect true orphaned templates
- Get template quality metrics
- Bulk operations via Management API

### Advanced Features
- Template export/import
- Batch enrollment
- Template quality analysis
- User enrollment history
- Template comparison tools

### Performance
- Pagination support
- Caching layer
- Async operations
- Batch processing

## Dependencies

- Keycloak 26.4.0+
- bioid-client module
- face-credential module
- JAX-RS API
- SLF4J logging

## License

See main project LICENSE file.

## Support

For issues and questions:
1. Check `docs/BWS_ADMIN_API.md`
2. Check `docs/BWS_ADMIN_SETUP_GUIDE.md`
3. Review application logs
4. Check Keycloak logs

## Contributing

1. Follow existing code style
2. Add tests for new features
3. Update documentation
4. Ensure security best practices
5. Test with multiple users

## Version History

### 1.0.0 (Current)
- Initial release
- Basic CRUD operations
- Role-based access control
- Audit logging
- Statistics dashboard
