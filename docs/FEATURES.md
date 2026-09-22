# FEATURES.md — Feature contracts (MVP)

Each feature traces to Problem_Statement.md §8. All decisions D1–D6 resolved; no UNKNOWN remains.

## F-01 Authentication — register / login (+ SMS OTP)
- Purpose: identify users with proven phone ownership. Actor: Customer, Admin.
- Preconditions: none.
- Register flow: submit name+phone+password → UNVERIFIED customer created → 6-digit code texted → verify → account activates AND session starts (201 challenge, then 200 JWT).
- Login flow: phone+password check → 6-digit code texted → verify → JWT (200 challenge, then 200 JWT). Correct password + unverified phone → 403 with a fresh REGISTER code (UI branches on 403).
- Rules (locked): codes SHA-256 at rest, 10-min TTL, 5 attempts then 410, single-active per (phone, purpose), REGISTER codes can't log in; wrong codes answer 401 uniformly; resend invalidates previous.
- Validation: phone format (`^\+?[0-9]{7,15}$`), password 8–100, unique phone (409).
- Authorization (D4): public register creates CUSTOMER only; role field is not accepted; admin comes from the V2 seed migration (grandfathered verified).
- Auth mechanism (D1): JWT HS256, 24h expiry, Bearer header. See DATA_API.md.
- Acceptance: no JWT without verified OTP; wrong password = 401 with friendly message; no password/token in logs (console-SMS prints the issued code by demo design).

## F-02 Browse services
- Actor: Customer (public read). Flow: GET services → list cards (name, duration, price, active only for customers; admins see inactive too).
- States: loading / list / empty ("No services yet") / error + retry.
- Dependencies: services table (docs/database-schema.md).

## F-03 View barbers
- Actor: Customer (public read). Flow: GET barbers → list (name, specialization, active only; admins see all). Same state matrix as F-02.

## F-04 Book appointment
- Actor: authenticated Customer.
- Preconditions: logged in (verified); active service + active barber selected; date >= today.
- Main flow: pick service → pick barber → pick date/time → POST /api/appointments → receive queue_number + status → show confirmation.
- Validation: required fields, ISO date/time, date today-or-later, same-day time must be future, service/barber IDs exist + active.
- Business rules (RESOLVED, TASK-005): D2 exact-slot block (same barber+date+time → 409 + V3 unique backstop); queue_number = per-day max+1 assigned at booking; new rows start QUEUED. estimatedWaitMinutes computed on read (TASK-007).
- Success: confirmation with queue number + detail link. Failure: 400 (validation), 401, 409 (conflict) with "slot taken, pick another", 500 friendly.
- DB: appointments row (user_id, barber_id, service_id, date, time, queue_number, status).

## F-05 View own appointments + track status
- Actor: authenticated Customer. Flow: GET own appointments → list with StatusBadge → detail view with queue number + live estimated wait (computed by the same queue formula, TASK-007).
- Authorization: backend scopes to caller ID (foreign/missing → uniform 404); frontend hiding is UX only.

## F-06 Admin: view all appointments
- Actor: Admin. Flow: GET all with optional date/status filters → backend-ordered table (locked TASK-006). No pagination in MVP (documented limitation; revisit if data grows).

## F-07 Admin: update appointment status
- Actor: Admin. Flow: select appointment → legal forward move → PATCH → row updates (locked TASK-006).
- Status set: QUEUED / IN_SERVICE / COMPLETED / CANCELLED. Legal: QUEUED→IN_SERVICE/CANCELLED, IN_SERVICE→COMPLETED/CANCELLED, terminal locked. Illegal → 409 naming the move.

## F-08 Admin: manage queue
- Actor: Admin. Rule: backend is source of truth for order/position. Frontend displays queue_number + status sorted per backend. No client-side reordering logic.
- Locked (TASK-007): `GET /api/queue?date=` returns backend-ordered rows with live waits (sum of service durations ahead on your barber's line; IN_SERVICE waits 0 but occupies; terminal 0/nothing; barbers independent). No manual reorder exists — flow moves via status transitions only.

## F-09/F-10 Admin catalog + F-11 Customer directory (locked TASK-006)
- Services/barbers: ADMIN CRUD; reads role-aware (customers see active only). Hard delete blocked while appointments reference the row (409, deactivate instead).
- Customers: ADMIN read-only list (D6); no writes.

## Cross-cutting acceptance (all features)
- Every API screen: loading / success / empty / validation-error / 401 / 403 / 404 / 409 / 500 / network-failure handling (UI_UX.md §3 state matrix).
- Backend validation enforced even if frontend validates (ENGINEERING.md).
