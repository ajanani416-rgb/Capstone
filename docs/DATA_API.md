# DATA_API.md — API contracts (evidence-based, all MVP endpoints locked)

## Verified
| Method | Endpoint | Auth | Result |
|---|---|---|---|
| GET | `/api/health` | none | 200 text (liveness; `/` serves the bundled UI) |
| POST | `/api/auth/register` | none | 201 challenge `{phone, purpose:REGISTER, expiresInSeconds}` (NO token); creates UNVERIFIED customer; 400 validation, 409 duplicate |
| POST | `/api/auth/login` | none | 200 challenge `{purpose:LOGIN}` after password check; 401 generic; 403 unverified (fresh REGISTER code, UI branches on status) |
| POST | `/api/auth/verify-otp` | none | 200 AuthResponse `{token,…}`; REGISTER activates; 400 shape, 401 wrong code, 410 dead code |
| POST | `/api/auth/otp/resend` | none | 200 fresh challenge (invalidates previous); REGISTER on verified → 400; LOGIN unknown/unverified → 401 generic |
| GET | `/api/auth/me` | Bearer | 200 `{id, name, phone, role}`; 401 otherwise |
| POST | `/api/appointments` | Bearer (books for self) | 201 `AppointmentResponse`; QUEUED + per-day queue#; 400/404/409/401 as documented |
| GET | `/api/appointments/me` | Bearer | 200 own rows ordered (empty `[]`) |
| GET | `/api/appointments/:id` | Bearer | 200 owner-or-admin (+live wait on QUEUED); foreign/missing → 404 |
| GET | `/api/appointments` | ADMIN | 200 ordered; `?date=&status=` filters; CUSTOMER → 403 |
| PATCH | `/api/appointments/:id/status` | ADMIN | 200; strict forward-only; illegal → 409, missing → 404 |
| GET/POST | `/api/services`, `/api/barbers` (+`/:id`) | public GET (role-aware), ADMIN writes | 200/201; 400; referenced DELETE → 409 |
| PUT/DELETE | `/api/services/:id`, `/api/barbers/:id` | ADMIN | 200 / 204 |
| GET | `/api/users` | ADMIN | 200 hash-free list; CUSTOMER → 403 |
| GET | `/api/queue?date=` (default today) | ADMIN | 200 ordered + live waits; CUSTOMER → 403 |

Auth rules: HS256 JWT 24h Bearer; no JWT without verified OTP; codes 6-digit SHA-256, 10-min TTL, 5 attempts→410, single-active per (phone, purpose). SMS: console sender default (demo; logs the code), gateway sender later via `app.sms.gateway`. Booking: ISO date/time, date≥today, active catalog, D2 exact-slot + V3 backstop. Queue: immutable order, sum-ahead waits, barbers independent. Build notes: `-parameters` flag (+explicit `@PathVariable` — unnamed ones 500'd); method-security denials rethrown (else 500).

## Standard envelope (enforced)
Success: resource JSON. Error: `{message, fieldErrors?}` 400/401/403/404/409/410/500. No stacks/secrets.

## UNKNOWN
None open for MVP. Watch: no pagination; migrations proven on H2-MySQL mode + MockMvc (first real MySQL boot: watch Flyway V1→V4 + `validate`).
