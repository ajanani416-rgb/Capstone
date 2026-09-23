# ENGINEERING.md — Implementation rules

## Backend
Layered Controller→Service→Repository; validation with `spring-boot-starter-validation` (`@Valid`, Bean Validation) enforced in Service too; transactions at Service (`@Transactional`) for booking + status changes; OTP bookkeeping in REQUIRES_NEW (throws must not roll back lockout); entities use indexes/unique constraints per locked rules; passwords BCrypt (never returned/logged), OTP codes SHA-256 (never logged, never in responses); JWT secret from env, 24h expiry; CORS allowlist (no `*` with credentials); `GlobalExceptionHandler` → clean error envelope, auth exceptions rethrown to Security translators.

## Frontend
React Router guards = UX only; `services/` layer owns all HTTP (baseURL via env, Bearer attach, single 401 handler → login redirect); `useAsync` loads once (predicate via ref); forms preserve input on failure, disable while pending; tables paginate (when added); no hardcoded secrets; env via `.env` + `.env.example` (never commit real values). Single-host: `VITE_API_URL=` + `static/` bundle + SPA fallback.

## Config / secrets
Never commit `.env`, JWT secrets, DB creds, SMTP passwords. `application.properties` uses `${VAR:default}` env style; checked-in defaults are dev-only. `.env.example` stays canonical. SMTP delivery needs operator Gmail App Password config; without it registration answers 502 (rolled back, safe retry).

## Email OTP
`EmailSender` interface; `SmtpEmailSender` (Gmail STARTTLS, HTML+text, fixed subject) is the only production sender; `MailConfig` builds the transport from env. AuthService never sees SMTP details. Tests mock `EmailSender` and read codes from the test-only OtpService hook.

## Quality gates (per task)
`mvn test`, `mvn clean package`, `npm run lint`, `npm run build` where scripts exist — all executed with outputs recorded, never claimed. Small Conventional Commits (`feat(appointments): …`). No silent changes to schema/API/auth/rules — update docs first (change control).
