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

## TASK-009 Email-OTP + UI refresh — DONE
V4 (verified + otp_codes, grandfathered), OtpService (SHA-256, 10-min, 5 tries, single-active; REQUIRES_NEW bookkeeping — same-txn rollback bug found by test, fixed), console/SMTP mail abstraction, auth rewrite (challenges, 403-unverified, resend), OtpStep UI + two-step pages, footer/session/stat cards/deepened accent/reduced-motion, single-host serving (static/ + SPA fallback + `/api/health`). Fixed useAsync infinite fetch loop (in-browser storm → single request). 37/37 green + live single-host proof (register→code→JWT→book→queue, in-browser to dashboard).

## DECISION_REQUIRED — none open (D1–D6 all resolved)
