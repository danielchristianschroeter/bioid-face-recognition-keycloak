# Product Requirements Document (PRD)
## Keycloak Extension – BioID BWS 3 Face Recognition (gRPC)

## 1. Purpose & Scope

The extension adds high-confidence face biometrics to Keycloak-based identity providers by integrating BioID's BWS 3 gRPC service. It must work as (a) a first-factor login alternative to passwords, (b) an adaptive second factor, and (c) a self-service enrollment / deletion module – all without storing raw biometric data inside Keycloak.

### Out-of-scope
• Face search / 1:N identification  
• On-device ML or WebAuthn extensions  
• Any storage of face images or templates outside BioID

### Success Metrics (12 weeks post-GA)
• ≥ 95 % enrollment completion rate (3 attempts max)  
• ≤ 3 s P95 verification latency (BioID gRPC RTT excluded)  
• ≤ 0.1 % false-accept rate at default threshold  
• ≤ 1 % admin-escalated deletion tickets older than 5 days  
• NPS ≥ +25 for end-user UX survey

## 2. Background & Problem Statement

Password fatigue, phishing, and MFA fatigue drives the need for frictionless, device-independent biometrics. Existing Keycloak deployments rely on OTP or WebAuthn, which require either hardware or high user effort. BioID offers proven liveness and privacy-preserving storage that addresses both usability and compliance gaps.

## 3. Definitions & Acronyms

- **BWS**: BioID Web Service  
- **CR**: Challenge–Response liveness (head turn)  
- **ClassId**: Opaque user template identifier in BioID  
- **TTL**: Template-Time-to-Live, defaults to 2 years  
- **PII**: Personally Identifiable Information  

## 4. Goals / Non-Goals

### 4.1 Goals
- **G1**: Seamless multi-image enrollment with passive feedback  
- **G2**: Fast 1:1 verification with configurable threshold  
- **G3**: Optional passive, active, or CR liveness modes  
- **G4**: GDPR-compliant deletion workflow with audit trail  
- **G5**: Works on Chromium-based, Safari, Firefox (desktop + mobile)  
- **G6**: No vendor lock-in in Keycloak source tree (packaged as JAR)  

### 4.2 Non-Goals
- **N1**: No batch import of existing templates  
- **N2**: No native mobile SDK; browser only  
- **N3**: No offline support  

## 5. Personas & Primary Use Cases

• **Corporate Employee "Alex"** – wants passwordless login on laptop  
• **Field Engineer "Sam"** – bad network, needs fallback to OTP  
• **Realm Admin "Dana"** – monitors enrollment completeness, must delete biometrics on termination requests  

## 6. User Stories (MoSCoW)

- **MUST-1**: As Alex, I can enroll my face in <60 s using my webcam.  
- **MUST-2**: As Alex, I can log in only with face on approved browsers.  
- **MUST-3**: As Sam, after 3 failed face attempts I am offered OTP.  
- **SHOULD-1**: As Dana, I see pending deletion requests and approve them.  
- **COULD-1**: As Dana, I download CSV report of template status.  
- **WON'T-1**: Support external camera calibration wizard (out-of-scope).  

## 7. Functional Requirements

### 7.1 Enrollment
- **F-E1**: Extension registers a RequiredActionProvider "face-enroll".  
- **F-E2**: Capture UI must collect ≥ 3 frames with distinct yaw angles (±30°).  
- **F-E3**: gRPC Enroll(ClassId, []ImageData) called once all frames buffered.  
- **F-E4**: Store template meta (createdAt, imageCount, bwsTemplateVersion) as Keycloak credential, NOT user-attribute to leverage encryption-in-db.

### 7.2 Verification / Login
- **F-V1**: Authenticator step "face-auth" pluggable anywhere in Browser Flow.  
- **F-V2**: On success, login continues; on fail > maxRetries, flow fallback triggers "Alternative" credential sub-flow.  
- **F-V3**: Score threshold default 0.015 (BioID scale), realm-configurable.  

### 7.3 Liveness
- **F-L1**: Passive liveness executes automatically (extra 200 ms) unless disabled.  
- **F-L2**: Active liveness prompts for smile; CR prompts for 2–4 random directions.  
- **F-L3**: Admin can enforce CR for high-risk clients via Condition-based Flow.  

### 7.4 Deletion & Re-enroll
- **F-D1**: Deletion requests logged as ADMIN_EVENT_PENDING.  
- **F-D2**: Approved deletion calls DeleteTemplate(ClassId) and purges credential row; declines retain template.  
- **F-D3**: Users may re-enroll immediately after deletion completes.  

### 7.5 Admin & Reporting
- **F-A1**: Realm settings panel ("Face Recognition") built with PatternFly, permission "manage-realm".  
- **F-A2**: Metrics exported via MicroProfile Metrics: enroll_success_total, verify_fail_total, average_verify_latency_ms.  

## 8. Non-Functional Requirements

- **NFR-1**: Latency: End-to-end verify flow ≤ 4 s P95 over 4G.  
- **NFR-2**: Availability: No additional SPOF; BioID endpoint health checked every 30 s.  
- **NFR-3**: Security: Mutual-TLS with client cert optionally supported; CBC/cha-poly encryption for in-flight gRPC.  
- **NFR-4**: Privacy: ClassId stored, no biometric template or raw images persisted.  
- **NFR-5**: Accessibility: WCAG 2.1 AA; provide speech prompts.  
- **NFR-6**: Localization: EN, DE, FR, ES JSON bundles out-of-box.  
- **NFR-7**: Cluster Awareness: Extension must be stateless; gRPC channel pool per node.  

## 9. API Contract Mapping

```
facerecognition.proto → Java (protobuf-3.25)  
• rpc Enroll(EnrollRequest) returns (EnrollResponse); timeout 7 s  
• rpc Verify(VerifyRequest) returns (VerifyResponse); timeout 4 s  
• rpc LivenessDetection(LivenessRequest) …  
• rpc VideoLivenessDetection(VideoLivenessRequest) …  
```

### Error mapping → Keycloak AuthenticationError:
- Code 4001 FACE_NOT_FOUND → ERR_FACE_NOT_FOUND (retryable)  
- Code 4005 MULTIPLE_FACES → ERR_MULTI_FACE (retryable)  
- Code 500x → ERR_BIOID_SERVICE (fallback path)  

## 10. Architecture

```
Keycloak Node
 ├── FaceAuthenticator (Authenticator SPI)  
 ├── FaceEnrollAction (RequiredAction SPI)  
 ├── FaceCredentialProvider (Credential SPI)  
 ├── BioIdClient (gRPC stub + retry w/ exponential backoff)  
 └── Metrics + EventListener
```

### Build & Packaging
• Maven module: keycloak-face-bioid  
• Externalized config in `${kc.home}/conf/bioid.properties`

### Deployment Topology
```
User ► Browser (WebCam) ► Keycloak (Extension) ───TLS──▶ BioID BWS gRPC  
Return gRPC ► Decision ► OIDC / SAML ► Application  
```

## 11. Security & Compliance

• DPIA required; sign-off by Privacy Office before GA.
• Terms of processing with BioID stored in Contract repo.
• Data-retention: template ttl default 2 years; logs 90 days.
• Security review includes OWASP ASVS L2 for UI, code-scan in pipeline.

## 12. Dependencies

- **D-1**: Keycloak ≥ 26.3.0 (SPI stability)
- **D-2**: Java 21 runtime
- **D-3**: gRPC-Java 1.73.x
- **D-4**: User browser must support MediaDevices.getUserMedia

## 13. Risks & Mitigations

- **R1**: Network latency → Use nearest-region BioID endpoint, pre-signed DNS Cf.
- **R2**: Browser restrictions on camera → Provide polyfill & fallback to file upload for desktop Safari <=15.
- **R3**: Data-protection concerns → Strict deletion workflow & DPIA.
- **R4**: Breaking changes in BioID proto → Pin API version; contract tests in CI.

## 14. Roll-Out Strategy

- **Phase 0 (Beta)**: Internal IAM realm only, 500 pilot users.  
- **Phase 1**: Opt-in for external partner realm; monitoring dashboards gated.
- **Phase 2**: Default enabled for new realms; existing realms notified 30 days prior.

## 15. Testing & QA

• Unit tests ≥ 80 % line coverage.  
• Contract tests with BioID staging env (GitHub Actions nightly).
• Browser matrix: Chrome, Edge, Firefox, Safari desktop + iOS 18.
• Accessibility test via axe-core.
• Pen-test by external vendor before Phase 2.

## 16. Documentation

• Admin Guide (asciidoc) – install, configure, troubleshoot.
• User Guide – enrollment & login steps with screenshots.
• API Guide – event types, metrics names.
Delivered in `/docs`.

## 17. Milestones

- **S1**: Proto client & Authenticator skeleton……………… 2 w  
- **S2**: Enrollment UI + gRPC Enroll………………………… 2 w  
- **S3**: Verify + Passive Liveness…………………………… 2 w  
- **S4**: Challenge-Response + admin config……………… 2 w  
- **S5**: Deletion workflow, audit, metrics………………… 1 w  
- **S6**: Hardening, cross-browser QA, docs………………… 2 w  

## 18. Acceptance Criteria

- **AC-01**: 3 distinct yaw images produce template ≥ 90 % of time.
- **AC-02**: Verification success within target latency; false accept ≤ 0.1 %.
- **AC-03**: Passive + active liveness toggles work, CR passes end-to-end.
- **AC-04**: Deletion calls confirmed by BioID API & credential row removed.
- **AC-05**: WCAG 2.1 AA automated checks pass (axe violations < 10).
- **AC-06**: All listed events visible in KC admin console and REST export.  
- **AC-07**: Extension runs in KC cluster (2 nodes) under k8s without sticky sessions.

## 19. Open Questions

- **Q1**: Should we auto-generate ClassId or reuse existing userId hashing?
- **Q2**: Define default verify threshold – need BioID to recommend for FAR 0.1 %.
- **Q3**: Does BioID support regional fail-over for GDPR data residency?