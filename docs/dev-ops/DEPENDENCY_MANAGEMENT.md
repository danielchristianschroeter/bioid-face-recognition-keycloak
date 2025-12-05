# Dependency Management Quick Reference

## 🚀 Quick Commands

### Check for Updates
```bash
# Automated check (recommended)
./scripts/check-dependencies.sh          # Linux/Mac
scripts\check-dependencies.bat          # Windows

# Manual checks
mvn versions:display-dependency-updates  # Show available updates
mvn versions:display-plugin-updates      # Show plugin updates
mvn org.owasp:dependency-check-maven:check  # Security scan
```

### Update Dependencies
```bash
# Automated update (recommended)
./scripts/update-dependencies.sh         # Linux/Mac

# Manual updates
mvn versions:use-latest-versions         # Update all (⚠️ risky)
mvn -Pupdate-dependencies               # Update minor/patch only (safer)
mvn versions:set-property -Dproperty=keycloak.version -DnewVersion=26.4.0
```

### Test After Updates
```bash
mvn clean compile                        # Test compilation
mvn test                                # Run unit tests
mvn verify -Pit                         # Run integration tests
mvn -Psecurity-scan                     # Security check
mvn -Pcode-quality                      # Code quality check
```

## 📊 Current Versions (as of last update)

| Dependency | Current | Latest Stable | Notes |
|------------|---------|---------------|-------|
| Keycloak | 26.4.0 | Check [releases](https://github.com/keycloak/keycloak/releases) | Core platform |
| gRPC | 1.76.0 | Check [releases](https://github.com/grpc/grpc-java/releases) | BioID communication |
| Protobuf | 4.33.0-RC2 | Check [releases](https://github.com/protocolbuffers/protobuf/releases) | Serialization |
| JJWT | 0.13.0 | Check [releases](https://github.com/jwtk/jjwt/releases) | JWT handling |
| BouncyCastle | 1.82 | Check [releases](https://www.bouncycastle.org/releasenotes.html) | Cryptography |
| JUnit | 6.0.0 | Check [releases](https://github.com/junit-team/junit5/releases) | Testing |
| Mockito | 5.20.0 | Check [releases](https://github.com/mockito/mockito/releases) | Mocking |

## 🔒 Security-Critical Dependencies

These dependencies should be updated immediately when security patches are available:

1. **Keycloak** - Core authentication platform
2. **BouncyCastle** - Cryptographic operations
3. **JJWT** - JWT token handling
4. **Jackson** - JSON processing (deserialization vulnerabilities)
5. **gRPC** - Network communication

## 🤖 Automated Processes

### Dependabot
- Runs weekly on Mondays
- Creates PRs for dependency updates
- Groups related dependencies
- Configured in `.github/dependabot.yml`

### Security Scanning
- OWASP Dependency Check runs weekly
- Scans on every PR that changes dependencies
- Fails build on CVSS 7+ vulnerabilities
- Reports uploaded to GitHub Security tab

### License Compliance
- Checks dependency licenses
- Ensures compatibility with project license
- Generates THIRD-PARTY.txt report

## 📋 Update Checklist

When updating dependencies:

- [ ] **Backup**: Create backup of pom.xml
- [ ] **Update**: Use scripts or manual commands
- [ ] **Compile**: `mvn clean compile`
- [ ] **Test**: `mvn test`
- [ ] **Integration**: `mvn verify -Pit`
- [ ] **Security**: `mvn -Psecurity-scan`
- [ ] **Quality**: `mvn -Pcode-quality`
- [ ] **Docker**: `docker compose build`
- [ ] **Documentation**: Update version requirements
- [ ] **Commit**: Git commit with clear message

## 🆘 Rollback Process

If updates cause issues:

```bash
# Restore from backup
cp pom.xml.backup pom.xml

# Or revert specific versions
mvn versions:set-property -Dproperty=PROPERTY_NAME -DnewVersion=OLD_VERSION

# Or use git
git checkout HEAD~1 -- pom.xml
```

## 📚 Resources

- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)
- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)
- [CVE Database](https://cve.mitre.org/)
- [Keycloak Security Advisories](https://github.com/keycloak/keycloak/security/advisories)

## 🔧 Troubleshooting

### Common Issues

**Compilation Errors After Update**
- Check breaking changes in release notes
- Update code to match new API
- Consider reverting to previous version

**Test Failures**
- Update test dependencies
- Check for behavior changes
- Update test expectations

**Security Vulnerabilities**
- Check if suppression is appropriate
- Update to patched version
- Consider alternative dependencies

**Version Conflicts**
- Use `mvn dependency:tree` to analyze
- Add explicit version management
- Exclude transitive dependencies if needed