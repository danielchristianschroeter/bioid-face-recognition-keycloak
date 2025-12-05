# Product Overview & Readiness Snapshot

This document replaces the older product notes and readiness checklists. The original deep dives now live in `docs/archive/product/` for historical reference.

## Value Proposition

- Passwordless sign-in powered by BioID face recognition, packaged as a Keycloak extension.
- Enterprise-grade privacy controls (GDPR deletion workflows, audit logging, configurable retention).
- Operations tooling for administrators: REST APIs, monitoring hooks, and troubleshooting playbooks.

See `archive/product/SUMMARY.md` for the original pitch deck content.

## Feature Inventory

- Core authentication: enrollment flow, adaptive verification thresholds, fallback paths.
- Liveness: passive + active smile + challenge-response with configurable policies.
- Admin UX: dashboard metrics, template lifecycle automation, bulk operations, REST APIs.
- User UX: account-console widgets for enrollment, status, and credential management.

Full matrix lives in `archive/product/CURRENT_FEATURES.md`, but the README + docs directory now reflect the maintained subset above.

## Requirements & Constraints

- Keycloak 26+, Java 21, BioID BWS credentials.
- Strict data protection rules (zero raw biometric persistence, encrypted artifacts, consent tracking).
- Scalability expectations: multi-region failover, rate limiting, and observability via Prometheus.

Historical requirement breakdown is stored in `archive/product/PRODUCT_REQUIREMENTS.md`.

## Production Readiness

- ✅ Dedicated deployment/operations guides cover Docker, manual installs, and REST usage.
- ✅ Monitoring/alerting via Micrometer + Prometheus exporters.
- ✅ Security posture documented in `docs/features/SECURITY.md`.
- 🔄 Remaining manual checks: organization-specific privacy reviews, Keycloak upgrade testing, performance baselining.

The retired audits (`PRODUCTION_READINESS_AUDIT.md` and `PRODUCTION_READY_VERIFICATION.md`) remain in the archive if you need the original questionnaires.

## Where to Start

1. Follow `docs/getting-started/SETUP.md` (Docker) or `docs/operations/DEPLOYMENT.md` (manual).
2. Configure BioID credentials and verify connectivity via the admin console health checks.
3. Use `docs/dev-ops/TESTING.md` for CI-friendly verification and `docs/operations/TROUBLESHOOTING.md` for incident response.

Refer back to the archive only when you need historical decisions or requirement traceability.
