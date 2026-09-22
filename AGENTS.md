# AGENTS.md — Instructions for implementation agents (OpenCode)

## Source of truth (precedence)
1. User's explicit instruction 2. Problem_Statement.md 3. docs/PRODUCT.md 4. FEATURES.md 5. UI_UX.md 6. SCREEN_SPECIFICATIONS.md 7. DESIGN_TOKENS.md 8. ARCHITECTURE.md 9. DATA_API.md 10. ENGINEERING.md 11. TESTING.md 12. TASKS.md 13. DEFINITION_OF_DONE.md 14. README.md 15. Existing code. On conflict: stop, report CONFLICT + affected area, do not silently pick.

## Protocols
UNKNOWN (no evidence), DECISION_REQUIRED (need human call — all D1–D6 resolved), CONFLICT (doc-vs-code), NOT_IMPLEMENTED (required but absent). Never invent requirements/fields/rules/endpoints/tests.

## Stack (locked)
Java 17, Spring Boot 3.2, Web/Data JPA/Security/Mail, MySQL, Maven; React+Vite+Router+CSS. No Node/Python backend, no framework swap, no microservices/Redis/Kafka/GraphQL/WS/payments without approval.

## Workflow per task
UNDERSTAND→AUDIT→SPECIFY→PLAN→IMPLEMENT→TEST→REVIEW→DOCUMENT→COMMIT. Smallest correct change; docs-first for schema/API/auth/rule changes; Conventional Commits; never commit `.env`/secrets/`static/`-bundles/H2-scope-widening; report per §58 with real command output only.

## Current state
TASK-001…009 DONE, 37 tests green, single-host demo proven. Local demo notes: H2 boot flags live in run commands (not committed); `static/` bundle is generated; temp H2 scope widening must be reverted before commit. Next: push to GitHub, real-MySQL boot check, Day-41/60 gates.
