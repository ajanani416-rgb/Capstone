# PRODUCT.md — Smart Salon Appointment & Queue Management System

Source hierarchy: Problem_Statement.md > this file. Where they conflict, Problem_Statement.md wins.

## 1. Product vision
REQUIREMENT (from Problem_Statement.md §5): A web-based system where customers register, book services, receive queue numbers, and track appointment status; admins manage queue, services, barbers, and appointment updates. Replaces phone/notebook/messaging-app booking.

## 2. Target users
REQUIREMENT (Problem_Statement.md §10):
- Customer: browse services, view barbers, book appointments, track status, view own appointments.
- Admin: view appointments, manage queue, update appointment status, manage services, manage barbers.
- Staff is covered by the Admin role (no separate role — locked).

## 3. User problems (from Problem_Statement.md §4)
- Double booking
- Long waiting times
- Poor queue visibility
- Difficulty managing appointments / tracking status
- Poor customer experience

## 4. User goals
- Customer: book without calling, prove email ownership via OTP, see queue number, know status/wait.
- Admin: see day's appointments, control queue order, update status, keep service/barber catalog correct.

## 5. Product goals
- Reduce appointment conflicts and waiting times (Problem_Statement.md §6).
- Single source of truth for appointments + queue (backend-owned).
- Understandable, defensible capstone MVP.

## 6. MVP scope (REQUIREMENT, Problem_Statement.md §7–§8)
1. Customer registration + email-OTP verification + login + OTP
2. Browse services + view barbers
3. Book appointment (service + barber + date + time) → queue number assigned
4. Customer: view own appointments, track status + live wait
5. Admin: view all appointments, manage queue, update status, manage services, manage barbers, customer directory
6. Live day queue with computed waits

## 7. Future scope (Problem_Statement.md §13 — explicitly NOT MVP)
Real-SMTP hardening/rotation policies, notifications/email reminders beyond OTP, analytics/prediction, payments, WebSocket live queue. Do not implement in MVP.

## 8. Out of scope / Non-goals
Chat, AI chatbot, recommendations, mobile app, microservices, advanced analytics, payments.

## 9. Business rules (all locked)
- Queue number assigned at booking: per-day max+1 (D5-numbering, TASK-005).
- Double-booking = same barber + date + time → 409 + DB unique backstop (D2, TASK-005).
- Status set QUEUED/IN_SERVICE/COMPLETED/CANCELLED; new bookings start QUEUED; strict forward-only transitions (D3, TASK-005/006).
- Wait = sum of service durations ahead on your barber's line, computed on read (D5-remainder, TASK-007). Order immutable, no manual reorder.
- No JWT without verified email OTP on both register and login (TASK-009).

## 10. Constraints
- Stack locked: Java 17, Spring Boot 3.2, Spring Web/Data JPA/Security, MySQL, React+Vite+React Router (Problem_Statement.md §11, backend/pom.xml verified).
- Backend owns queue ordering. Frontend displays only.

## 11. Assumptions (explicit)
- A1: Single salon (no multi-tenant). No evidence of multi-salon requirement.
- A2: Timezone is server-local; no evidence of multi-timezone need.

## 12. Success criteria (MVP acceptance)
- Customer can register → verify email → login (+OTP) → browse → book → see queue number + live wait.
- Admin can login (+OTP) → see appointments → update status → manage services/barbers → view queue + customers.
- No double-booking under the exact-slot rule.
- Responsive 390/768/1024/1440, loading/empty/error states on every API screen.
