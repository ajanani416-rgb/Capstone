# Smart Salon Appointment and Queue Management System

A college Full Stack capstone MVP: customers register, verify their phone number via SMS OTP, browse services/barbers, book appointments, receive per-day queue numbers, and track live wait estimates; admins manage appointments, the day queue, status lifecycle, catalog, and a read-only customer directory. JWT(+OTP)-secured Spring Boot REST API + React/Vite frontend + MySQL/Flyway. All 13 Problem_Statement functional requirements implemented; 37 backend tests green.

## Features
- Customer: register + SMS OTP, login + OTP, browse services & barbers, book (exact-slot conflict → friendly 409), own appointments, live queue wait on detail + today view
- Admin: filtered appointment list, strict status lifecycle (QUEUED→IN_SERVICE/COMPLETED, CANCELLED), day queue with computed waits, services/barbers CRUD (referenced-delete blocked), customer directory
- Auth: passwordless — 6-digit SMS OTP (2-min, 5 tries) before register-complete AND every login, then JWT (24h Bearer); seeded admin; CUSTOMER-only public registration; JSON 401/403/410
- Frontend: role-based routes/layouts, design-token CSS, OTP code entry, loading/empty/error+retry on every API view, responsive 390–1440
- Docs: full spec set under `docs/` (PRODUCT → DEFINITION_OF_DONE) + diagrams

## Technology Stack
- Backend: Java 17, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Spring Security, Spring Validation, MySQL, Flyway
- Frontend: React, Vite, JavaScript, React Router, CSS
- Documentation: Markdown

## Architecture
React frontend communicates with a REST API built with Spring Boot. The application is organized with controllers, services, repositories, and entity layers. Single-host serving supported: the backend bundles the React app (`SpaFallbackController`, `/api/health` probe).

## Database
MySQL is used as the relational database. Flyway owns migrations (V1 schema, V2 admin seed, V3 slot-unique, V4 OTP, V5 admin identity, V6 phone identity); Hibernate validates (`ddl-auto=validate`).

## Folder Structure
- `backend/` - Spring Boot backend project
- `frontend/` - React + Vite frontend project
- `docs/` - project documentation and diagrams
- `Problem_Statement.md` - capstone problem statement
- `.env.example` - environment variable template

## Prerequisites
- Java 17+
- Maven
- Node.js 18+ and npm
- MySQL

## How to run backend
1. Configure env (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`) or edit `backend/src/main/resources/application.properties`.
2. Run from `backend/`:
   - `mvn clean test`
   - `mvn spring-boot:run`

## How to run frontend
1. From `frontend/`:
   - Copy `.env.example` to `.env` and set `VITE_API_URL` (defaults to `http://localhost:8080`).
   - `npm install`
   - `npm run lint`
   - `npm run build`
   - `npm run dev`

## MySQL setup
1. Create a database named `salon_management`.
2. Update `.env` or `backend/src/main/resources/application.properties` with your connection settings.
3. Flyway runs V1 (schema) → V7 (passwordless) on startup, incl. admin seed. Dev admin phone: `+910000000001` (SMS OTP from the backend log, no password) — change the number immediately in any shared environment.

## Environment variables
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (≥32 chars; dev default in properties is local-only)
- `VITE_API_URL` (frontend)

## Demo walkthrough (capstone review, ~5 min)
1. Boot backend + open `/services` (public catalog).
2. Register a customer (name + phone) → read the 6-digit code from the backend log → verify → auto-logged in to `/customer`; book a service for tomorrow → note the queue number.
3. Log out, log back in → enter the number, then the SMS code again → session.
4. Book the same barber/slot again → friendly "already booked" 409, selections kept.
5. Log in as admin (`+910000000001`, code from backend log) → dashboard counts → `/admin/queue` shows the wait math → move the appointment QUEUED→IN_SERVICE→COMPLETED (try skipping to COMPLETED: 409).
6. `/admin/services`: deactivate a service → it vanishes from public catalog; try deleting a referenced one → 409 with remedy.

## Known limitations (honest scope)
- No pagination on admin lists (fine at MVP scale; revisit with growth).
- Wait estimates are same-day, per-barber duration sums — no-shows/overruns aren't modeled.
- Queue order immutable by design; no WebSocket live updates (poll/refresh), no notifications beyond OTP, no payments/analytics (all listed as future scope in Problem_Statement).
- Console SMS sender is demo-only; add a gateway sender for anything shared.
- Dev admin phone is committed; rotate before any shared demo.
