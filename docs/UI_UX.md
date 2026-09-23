# UI_UX.md — Navigation, journeys, states

Reference (design direction only, not a copy): Dribbble shot 25812917 "POS Dashboard — Salon POS" by Master Creationz: dark sidebar nav + light content canvas, greeting/summary cards on top, central data table (services), right-rail performance donut + totals, pill status chips, rounded cards. Inspo archive consensus (ochre/charcoal, grotesk-sans) and ui-ux-pro dashboard checklist (stat cards, micro-hover, footer, reduced-motion) refined the system in TASK-009. What we borrow: information hierarchy (nav / summary / table / side status), density, card+table pattern. What we do NOT borrow: POS sales-analytics content, fake revenue figures — our content comes from real backend data only.

## 1. Navigation
- Public: `/`, `/login`, `/register`, `/services` (browse, read-only).
- Customer (auth): `/customer`, `/customer/book`, `/customer/appointments`, `/customer/appointments/:id`, `/customer/queue`.
- Admin (auth + role=ADMIN): `/admin`, `/admin/appointments`, `/admin/queue`, `/admin/services`, `/admin/barbers`, `/admin/customers`.
- Rules: navbar shows role-appropriate links; sidebars carry brand mark, section label, session user + logout; route guards are UX only — backend enforces authZ (ENGINEERING.md). 404 page for unknown routes; 403 page for wrong role.

## 2. User journeys (approved flows only)
Customer:
`Landing → Register (name/email/password) → Email OTP → Dashboard → Services → Book (service→barber→date/time→confirm) → Appointment detail (queue number + live wait) → Queue/status`
Login: `Login (email/password) → Dashboard` (unverified → verification screen)
Admin:
`Login (email/password) → Dashboard (today's load) → Appointments (filter) → Queue (status updates) → Services/Barbers (CRUD) → Customers`
- Never expose internal IDs as the primary identifier to customers; show service/barber names, date/time, queue number, status.

## 3. Required states per API screen
`loading → success | empty | error(retry)`. Plus: validation-error (inline, field-level), 401 (redirect login), 403 (not-allowed page), 404 (not-found page), 410 (OTP dead → resend), network-failure (retry). Tables: skeleton or spinner → rows | "No X yet + CTA" | "Couldn't load + Retry". Forms: labels, required markers, inline errors, disabled submit while pending, preserve input on failure, success toast + redirect. OTP: 6 boxes with paste/backspace support, countdown, resend cooldown.

## 4. Responsive (390 / 768 / 1024 / 1440)
- ≤768: sidebar → top navbar + hamburger or bottom nav; tables → stacked cards (AppointmentCard) — never horizontal scroll; donut/side-rail stacks below main content; touch targets ≥44px.
- 768–1024: two-column (content + rail), collapsible sidebar.
- ≥1024: three-region admin layout (sidebar / table / status rail) echoing reference hierarchy.
- No horizontal overflow, no overlapping, forms single-column on mobile.

## 5. Accessibility
Semantic landmarks (header/nav/main/table), `<label>` on every input, visible focus rings, keyboard-operable nav/modal/date pickers, OTP boxes group-labelled with per-digit labels, status conveyed by text + icon (never color alone), contrast ≥4.5:1 (accent deepened to #9A6206), alt text on imagery, aria-live for queue/status updates and form errors, `prefers-reduced-motion` disables micro-interactions.
