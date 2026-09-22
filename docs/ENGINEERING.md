# ENGINEERING.md — Implementation rules

## Backend
Layered Controller→Service→Repository; validation with `spring-boot-starter-validation` (`@Valid`, Bean Validation) enforced in Service too; transactions at Service (`@Transactional`) for booking + status changes; OTP bookkeeping in REQUIRES_NEW (throws must not roll back lockout); entities use indexes/unique constraints per locked rules; password hashing (BCrypt, never log); OTP codes SHA-256 (never log except console-mail by demo design); JWT secret from env, 24h expiry; CORS allowlist (no `*` with credentials); `GlobalExceptionHandler` → clean error envelope, auth exceptions rethrown to Security translators.

## Frontend
React Router guards = UX only; `services/` layer owns all HTTP (baseURL via env, Bearer attach, single 401 handler → login redirect); `useAsync` loads once (predicate via ref); forms preserve input on failure, disable while pending; tables paginate (when added); no hardcoded secrets; env via `.env` + `.env.example` (never commit real values). Single-host: `VITE_API_URL=` + `static/` bundle + SPA fallback.

## Config / secrets
Never commit `.env`, passwords, JWT secrets, DB creds, SMTP passwords. `application.properties` uses `${VAR:default}` env style; checked-in defaults are dev-only. `.env.example` stays canonical. Console OTP sender is demo-only; SMTP (`app.mail.*`) for anything shared.

## Mail
`OtpMailSender` interface; console default; SMTP active when `app.mail.host` set (Gmail: app password, port 587, STARTTLS). Both conditions in `OtpMailConfig` (ordering-safe).

## Quality gates (per task)
`mvn test`, `mvn clean package`, `npm run lint`, `npm run build` where scripts exist — all executed with outputs recorded, never claimed. Small Conventional Commits (`feat(appointments): …`). No silent changes to schema/API/auth/rules — update docs first (change control).
