# ARCHITECTURE.md — Approved technical architecture (MVP)

## 1. High-level (locked)
`User → React+Vite → HTTP/REST → Spring Boot (Controller→Service→Repository) → JPA/Hibernate → MySQL`. Never React→MySQL. Queue ordering computed backend-side; frontend displays. Single-host serving: the backend also serves the bundled React app (`static/` + `SpaFallbackController`); `/api/*` stays pure API.

## 2. Backend layers (package `com.salon.management`)
Controller (HTTP, routing, basic validation) → Service (business rules, transactions; OTP bookkeeping in REQUIRES_NEW via OtpCodeStore so throws can't roll back lockout) → Repository (Spring Data JPA interfaces) → Entity (persistent model). DTOs at API boundary; entities never serialized directly. `GlobalExceptionHandler` maps exceptions → `{message, fieldErrors}` without stack traces; auth exceptions rethrown to Spring Security translators (method-level denials must 401/403, never 500). Mail leaves through the `EmailSender` interface (SMTP impl; mocked in tests). Current state: all layers implemented, 40 tests green.

## 3. Frontend layers (`frontend/src/`)
`pages/ → components/ → services/ → routes/`, plus `layouts/ hooks/ auth/ utils/ styles/`. API calls only via `services/*.js` (baseURL via env, Bearer attach from tokenStore, single 401 handler). `useAsync` loads once (predicate via ref — inline callbacks must never retrigger fetches). No business-rule duplication.

## 4. AuthN/Z (locked: JWT + email/password + email OTP, D1)
Spring Security stateless + JWT (+ BCrypt `PasswordEncoder` bean); register returns an emailed-OTP challenge, `verify-otp` mints the JWT, verified password login mints it directly. Public: `/`, SPA routes, `/api/health`, POST `/api/auth/**`, GET catalog. Everything else needs a bearer token; writes + admin reads additionally `@PreAuthorize("hasRole('ADMIN')")`. Frontend guards are UX; backend matchers + method security enforce. Secrets via env only (`DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/MAIL_USERNAME/MAIL_PASSWORD`).

## 5. Data (see database-schema.md + migrations V1→V9)
users (email identity, BCrypt password_hash, email_verified) / services / barbers / appointments (V3 slot-unique) / otp_codes (email-keyed, hashed, TTL, attempts). JPA: `@ManyToOne` appointment→each parent; avoid N+1 via fetch discipline. Flyway owns schema; `ddl-auto=validate`.

## 6. What we do NOT build
Microservices, K8s, Redis/Kafka, GraphQL, WS live updates (poll instead), payments, analytics engine.
