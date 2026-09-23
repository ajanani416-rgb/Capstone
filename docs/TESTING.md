# TESTING.md — Strategy (MVP)

## Tools (locked to stack)
Backend: JUnit 5 + `spring-boot-starter-test` (+ MockMvc), H2 in PostgreSQL mode for slice/context tests + Flyway-on-H2 migration test. Frontend: oxlint + `vite build` gates; browser proof via Playwright screenshots/flows for releases. No E2E harness in repo (manual 17-case sheet + demo script cover it).

## Coverage (42 tests, all green at last run)
Auth: register-challenge/verify/password-login/403-unverified/wrong-password-401/unknown-email-401/wrong-code-401/duplicate-409/role-smuggle/SMTP-failure-502/me-401/400-fields/health (AuthApiTest 11). OTP: resend-invalidates/cooldown-429/5-tries→410/expiry→410/purpose-isolation/resend-after-verified-400/malformed-400 (OtpApiTest 7). Booking: happy+queue#/conflict-409/past-400/unknown-404/inactive-400/isolation/401 (BookingApiTest 6). Admin: filters/403s/legal+illegal moves/catalog CRUD+delete-block/users-no-secrets (AdminApiTest 6). Queue: formula/in-service/terminal/independence/today+403/detail-wait (QueueApiTest 5). Persistence: graph/unique-email/active-filters/ordering (DomainPersistenceTest 4). Migrations: V1→V9 + seed-once + V3 backstop (MigrationTest 1). Mail: multipart-alternative content + transport-failure mapping (SmtpEmailSenderTest 2, no context/network). SMTP is mocked per API test class; no test touches a real inbox. Note: `src/test/resources/application.properties` shadows the main one by filename in tests (same-classpath rule) — context tests run on @Value defaults + test props, which is why the real sender is covered by a plain unit test instead.

## Current state
Unit/contract suites green. Open: real-PostgreSQL boot check, Vitest only if justified, Playwright flows codified only if repeated. Never claim pass without execution.
