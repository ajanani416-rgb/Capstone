# SCREEN_SPECIFICATIONS.md — MVP screens

Route map mirrors the React Router tree. Auth column = backend enforcement (frontend guard is UX only).

## S-01 Landing `/` (public intro)
Order per brief: appointment-story hero (Login/Register top-right in navbar) → live services preview (real catalog) → role tracks (client books / admin approves from schedule) → approval flow strip → CTA band → footer. Empty catalog hides the preview section. Responsive: hero stacks ≤768px.

## S-02 Register `/register`, S-03 Login `/login` (public)
Register: name/phone with inline validation → 201 challenge → OTP step (6 boxes, countdown, resend) → verify → auto-login → role landing. Login: phone → challenge → OTP step → session; 403 unverified jumps to REGISTER OTP. 409 phone-taken; 401 unknown number; input preserved on failure; submit disabled while pending.

## S-04 Customer Dashboard `/customer` (CUSTOMER)
Next appointment + queue position + quick actions. States: loading / has-appointment / none ("No appointments yet — Book your first") / error+retry.

## S-05 Browse `/services` + book entry `/customer/book` (CUSTOMER; browse public)
Book: service+barber selects (live catalog), native date (`min=today`) + time inputs, no invented slot grid; 409 conflict keeps selections with remedy message.

## S-06 My Appointments `/customer/appointments`, S-07 Detail `:id` (CUSTOMER, owner-scoped)
List rows (service, barber, date/time, queue #, StatusBadge). Detail: full fields + live estimated wait + status. 403/404 pages as applicable.

## S-08 My Queue `/customer/queue` (CUSTOMER)
Own today's appointments, backend-ordered. Full day queue stays admin-only.

## S-09 Admin Dashboard `/admin` (ADMIN)
Stat cards (total today, queued, in-service) with icon/label/value/link — real numbers only. Empty: "No appointments today".

## S-10 Admin Appointments `/admin/appointments` (ADMIN)
Date/status filters + legal-move buttons only (terminal rows show "Terminal"); optimistic update with rollback; 409 message inline per row.

## S-11 Admin Queue `/admin/queue` (ADMIN)
Position, service, barber, time, queue #, live wait, status. Backend order, never re-sorted.

## S-12 Services `/admin/services`, S-13 Barbers `/admin/barbers` (ADMIN CRUD)
Table + modal form (full fields incl. active toggle) + two-step delete (referenced → 409 remedy). Validation per F-09/F-10.

## S-14 Customers `/admin/customers` (ADMIN, read-only)
ID/name/phone/role/since table. No writes by design (D6).

Shared: every screen implements UI_UX.md §3 states; tables collapse to cards ≤768px.
