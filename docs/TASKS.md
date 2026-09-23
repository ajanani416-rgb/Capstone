# TASKS.md — Executable task list (TASK-001…TASK-009 DONE)

## TASK-001 Phase-2 docs foundation — DONE
Spec set (PRODUCT→DEFINITION_OF_DONE + AGENTS) from repo evidence; unknowns logged as D1–D6.

## TASK-002 Phase-3 backend domain — DONE
Entities (User/Role/Service/Barber/Appointment), repositories, V1 baseline, env-ified config, placeholders deleted. 4/4 green.

## TASK-003 Phase-4 frontend shell — DONE
Vite+React 19+Router, routes S-01…S-13, layouts, tokens, 9 components, useAsync, services. Lint 0, build ok.

## TASK-004 Auth slice — DONE (D1=JWT, D4=seed admin)
JwtService/filter/chain, BCrypt, AuthController, envelope handler, V2 seed, frontend wiring. Fixed public-`/me` hole. 12/12 green.

## TASK-005 Booking — DONE (D2 exact-slot, D3-set, per-day queue#)
Service pre-check + V3 unique, QUEUED start, owner-scoped reads, native date/time pickers. Fixed unnamed-`@PathVariable` 500s (`-parameters` flag). 18/18 green.

## TASK-006 Admin — DONE (D3-guard strict, D6 read-only users)
Filters, transition guard → 409, catalog CRUD + referenced-delete block, method security, users list. Fixed method-security 500-instead-of-403. 24/24 green.

## TASK-007 Live queue — DONE (D5: sum-ahead waits, immutable order)
QueueService + `/api/queue`, live wait on detail, wait column. Fixed shared-H2 status-filter isolation. 29/29 green.

## TASK-008 Final review — DONE
Five perspectives, dead-code removal, README demo + limitations, preview-200 proof.

## TASK-009 Email-OTP + UI refresh — DONE (superseded by TASK-010)
V4 (verified + otp_codes, grandfathered), OtpService (SHA-256, 10-min, 5 tries, single-active; REQUIRES_NEW bookkeeping — same-txn rollback bug found by test, fixed), console/SMTP mail abstraction, auth rewrite (challenges, 403-unverified, resend), OtpStep UI + two-step pages, footer/session/stat cards/deepened accent/reduced-motion, single-host serving (static/ + SPA fallback + `/api/health`). Fixed useAsync infinite fetch loop (in-browser storm → single request). 37/37 green + live single-host proof (register→code→JWT→book→queue, in-browser to dashboard).

## TASK-010 SMS-OTP identity (phone replaces email) — DONE
Operator decision: Gmail OTP removed entirely (mail package + starter-mail deleted), phone is the login identity and SMS OTP channel. V6 (users.email→phone incl. admin backfill `+910000000001`, otp_codes.email→phone with V4-index drop first — H2 refuses referenced-column drops), entities/DTOs/repos/services/exceptions renamed to phone, console SMS sender (log-only, gateway later via `app.sms.gateway`), frontend auth pages + OtpStep + customers table to phone, all docs updated. 37/37 green.

## TASK-011 Passwordless auth (phone + SMS OTP only) — DONE (superseded by TASK-012)
Operator decision: passwords removed entirely. V7 drops `users.password_hash`; User/DTOs lose password fields; AuthService login is number-lookup + challenge (no password check); PasswordEncoder bean deleted; frontend register is name+phone, login is phone-only ("Text me a code"); all tests + docs updated. 37/37 green.

## TASK-012 Email OTP + SMTP authentication — DONE
Operator decision: phone/SMS removed entirely (sms package deleted), email is the identity, password the credential, email OTP the verification. starter-mail added; `EmailSender` + `SmtpEmailSender` (Gmail STARTTLS, HTML+text, fixed subject) + `MailConfig` (env creds); BCrypt `PasswordEncoder` restored; User gains email (nullable UNIQUE — pre-V9 phone-only demo rows survive but must re-register)/password_hash/email_verified, OtpCode keyed by email; V9 migrates (verified→email_verified carried, admin backfilled ajanani416@gmail.com + V2 hash + verified, phone columns dropped); OtpService TTL 10 min + 60s server resend cooldown (429) + SHA-256 + SecureRandom kept; login returns JWT for verified accounts, 403+fresh-code for unverified; SMTP failure → 502 with rollback; frontend email/password + verification screen (masked address, 600s countdown, 60s resend); tests mock EmailSender (40/40 green, incl. cooldown-429 + SMTP-502).

## DECISION_REQUIRED — none open (D1–D6 all resolved)
