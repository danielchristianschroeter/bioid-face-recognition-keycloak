# Failed Authentication Storage Overview

This document summarizes the investigation and implementation history for the failed-authentication storage feature. Detailed design notes, architecture diagrams, and test logs now live in `docs/archive/failed-auth/`.

## Why the Feature Exists

- Reduce repeated failed logins by letting support teams inspect biometric failures.
- Capture high-quality failure imagery so users can selectively improve their templates.
- Preserve forensic evidence with auditable history that respects GDPR retention policies.

Business impact (see `FAILED_AUTH_IMAGE_STORAGE.md` in the archive for details):

- 60–80% fewer “can’t log in” support tickets through self-service remediation.
- Higher trust thanks to transparent review flows and privacy controls.
- Continuous template improvement as users contribute edge-case samples.

## Architecture Decisions

1. **Hybrid storage strategy**

   - Metadata and counters stay on the Keycloak side for cluster awareness.
   - Encrypted images live on shared storage (NFS/S3) using a deterministic folder hierarchy.
   - Rationale: fastest MVP while leaving space to migrate into a dedicated JPA provider.

2. **Dedicated PostgreSQL service (Phase 2)**

   - `postgres-failedauth` container exposed on port `5433`.
   - Hibernate + C3P0 manage persistence with environment-driven configuration.
   - Ensures isolation from the main Keycloak DB, with schema auto-management.

3. **Future-proof entities**
   - Tables for attempts, images, audit logs, user preferences, and realm config already modeled.
   - Supports retention policies, review workflows, enrollment tagging, and high-volume querying.

See `FAILED_AUTH_ARCHITECTURE_UPDATE.md` and `FAILED_AUTH_JPA_PROVIDER_COMPLETE.md` in the archive for the full decision log.

## Implementation Status

- ✅ File-system prototype for encrypted image capture.
- ✅ EntityManagerFactory bootstrap with externalized credentials.
- ✅ 74 unit tests covering configuration, entities, encryption, and image processing.
- ✅ Docker Compose service + environment variables documented.
- 🔄 Pending: admin UI wiring, user-facing review screens, migration tooling.

Progress snapshots and task breakdowns are recorded in `FAILED_AUTH_PROGRESS_SUMMARY.md` and `FAILED_AUTH_IMPLEMENTATION_STATUS.md`.

## Operational Notes

- Use `FAILED_AUTH_DB_SCHEMA_UPDATE=update` in development; plan for migrations in production.
- Monitor pool metrics and Postgres backups separately from the primary Keycloak database.
- Follow the retention policies documented in `FAILED_AUTH_IMAGE_STORAGE.md` to keep storage costs bounded.

## Where to Dive Deeper

- `docs/archive/failed-auth/FAILED_AUTH_FIX_SUMMARY.md` – Root-cause analysis for the original provider misconfiguration.
- `docs/archive/failed-auth/FAILED_AUTH_IMAGE_STORAGE.md` – Detailed metadata schema and REST flows.
- `docs/archive/failed-auth/FAILED_AUTH_IMPLEMENTATION_GUIDE.md` – Step-by-step deployment checklist.
- `docs/archive/failed-auth/FAILED_AUTH_PROGRESS_SUMMARY.md` – Timeline with open items.

Use this overview as the canonical entry point and reference the archived files for exhaustive context when needed.
