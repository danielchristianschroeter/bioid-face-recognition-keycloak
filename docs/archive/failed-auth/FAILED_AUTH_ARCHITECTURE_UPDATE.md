# Failed Authentication Storage - Architecture Update

## Issue Discovered

During implementation, we discovered that Keycloak's architecture doesn't allow direct JPA/EntityManager access from extension code. Keycloak uses its own data access patterns through providers.

## Architectural Decision

We have two options:

### Option 1: Use Keycloak User Attributes (Recommended for MVP)
**Pros**:
- Works with Keycloak's architecture
- No custom database tables needed
- Cluster-friendly (uses Keycloak's replication)
- Faster to implement

**Cons**:
- Limited query capabilities
- Attributes stored as strings (need JSON serialization)
- Less efficient for large datasets

### Option 2: Custom JPA Provider (Full Implementation)
**Pros**:
- Full database capabilities
- Efficient queries and indexing
- Better for large-scale deployments

**Cons**:
- More complex implementation
- Requires custom Keycloak provider
- Longer development time

## Recommendation: Hybrid Approach

**Phase 1 (MVP)**: Use User Attributes
- Store failed attempt metadata in user attributes
- Store images in file system (encrypted)
- Quick to implement and test

**Phase 2 (Scale)**: Migrate to Custom JPA Provider
- Implement custom Keycloak provider for database access
- Migrate existing data
- Full query capabilities

## Updated Implementation Plan

### Storage Strategy
```
User Attributes (Keycloak):
- failed.auth.attempts.{attemptId} = JSON metadata
- failed.auth.count = total count
- failed.auth.last = last attempt timestamp

File System (Encrypted):
- /opt/keycloak/failed-auth-images/{realm}/{userId}/{attemptId}/
  ├── image-0.enc (encrypted image)
  ├── thumb-0.enc (encrypted thumbnail)
  ├── image-1.enc
  ├── thumb-1.enc
  └── metadata.json
```

### Benefits of Hybrid Approach
1. **Quick MVP**: Can implement and test immediately
2. **Cluster-Friendly**: File system can be on shared storage (NFS, S3)
3. **Scalable**: Can migrate to database later without API changes
4. **Flexible**: Easy to add/remove features

## Next Steps

1. Simplify FailedAuthImageStorageService to use file system
2. Use Keycloak user attributes for metadata
3. Implement and test MVP
4. Plan migration to full JPA provider if needed

## Decision

**Proceed with Hybrid Approach** for faster delivery and easier testing.

