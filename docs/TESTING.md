# TESTING.md — Strategy (MVP)

## Tools (locked to stack)
Backend: JUnit 5 + `spring-boot-starter-test` (+ MockMvc), H2 for slice/context tests + Flyway-on-H2 migration test. Frontend: oxlint + `vite build` gates; browser proof via Playwright screenshots/flows for releases. No E2E harness in repo (manual 17-case sheet + demo script cover it).

## Coverage (37 tests, all green at last run)
Auth: register-challenge/verify/login-challenge/403-unverified/duplicate-409/role-smuggle/bad-creds-401/me-401/400-fields/health (AuthApiTest 9). OTP: resend-invalidates/5-tries→410/expiry→410/purpose-isolation/resend-after-verified-400/malformed-400 (OtpApiTest 6). Booking: happy+queue#/conflict-409/past-400/unknown-404/inactive-400/isolation/401 (BookingApiTest 6). Admin: filters/403s/legal+illegal moves/catalog CRUD+delete-block/users-no-secrets (AdminApiTest 6). Queue: formula/in-service/terminal/independence/today+403/detail-wait (QueueApiTest 5). Persistence: graph/unique-email/active-filters/ordering (DomainPersistenceTest 4). Migrations: V1→V4 + seed-once + V3 backstop (MigrationTest 1).

## Current state
Unit/contract suites green. Open: real-MySQL boot check, Vitest only if justified, Playwright flows codified only if repeated. Never claim pass without execution.
