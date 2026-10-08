# Smart Salon Appointment and Queue Management System

A college Full Stack capstone MVP: customers register with email + password, verify their email via SMTP OTP, browse services/barbers, book appointments, receive per-day queue numbers, and track live wait estimates; admins manage appointments, the day queue, status lifecycle, catalog, and a read-only customer directory. JWT(+email-OTP)-secured Spring Boot REST API + React/Vite frontend + PostgreSQL/Flyway. All 13 Problem_Statement functional requirements implemented; 47 backend tests green (42 unit/contract + 5 PG-gated integration).

## Features
- Customer: register (email+password) + email OTP verification, password login, browse services & barbers, book (exact-slot conflict → friendly 409), own appointments, live queue wait on detail + today view
- Admin: filtered appointment list, strict status lifecycle (QUEUED→IN_SERVICE/COMPLETED, CANCELLED), day queue with computed waits, services/barbers CRUD (referenced-delete blocked), customer directory
- Auth: email/password + 6-digit email OTP (10-min TTL, 5 tries, 60s resend cooldown) before register-complete, then JWT (24h Bearer); verified password login; seeded verified admin; CUSTOMER-only public registration; JSON 401/403/410/429/502
- Frontend: role-based routes/layouts, design-token CSS, email verification screen (masked address, 6 boxes, 10-min countdown, 60s resend), loading/empty/error+retry on every API view, responsive 390–1440
- Docs: full spec set under `docs/` (PRODUCT → DEFINITION_OF_DONE) + diagrams

## Technology Stack
- Backend: Java 17, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Mail (SMTP), PostgreSQL, Flyway
- Frontend: React, Vite, JavaScript, React Router, CSS
- Documentation: Markdown

## Architecture
React frontend communicates with a REST API built with Spring Boot. The application is organized with controllers, services, repositories, and entity layers. Single-host serving supported: the backend bundles the React app (`SpaFallbackController`, `/api/health` probe).

## Database
PostgreSQL is used as the relational database. Flyway owns migrations (V1 schema, V2 admin seed, V3 slot-unique, V4 OTP, V5 admin identity, V6 phone identity, V7 passwordless, V9 email identity); Hibernate validates (`ddl-auto=validate`).

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
- PostgreSQL
- A Gmail account + App Password (for SMTP OTP delivery)

## How to run backend
1. Configure env (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`) or edit `backend/src/main/resources/application.properties`.
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

## PostgreSQL setup
1. Install PostgreSQL 16 and start it (`brew install postgresql@16 && brew services start postgresql@16`, or Postgres.app).
2. Create a database and user: `createdb salon_management` (default user `postgres`).
3. Update `.env` or `backend/src/main/resources/application.properties` with your connection settings (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
3. Create a Gmail App Password (Google Account → Security → 2-Step Verification → App passwords) and set `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM`.
4. Flyway runs V1 (schema) → V9 (email identity) on startup, incl. admin seed. Dev admin: `ajanani416@gmail.com` / `Admin@123` (DEV-ONLY default — change immediately in any shared environment).

## Environment variables
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (≥32 chars; dev default in properties is local-only)
- `MAIL_HOST` (default `smtp.gmail.com`), `MAIL_PORT` (default `587`)
- `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail App Password — never commit), `MAIL_FROM`, `MAIL_FROM_NAME`
- `APP_OTP_TTL_MINUTES` (default `10`), `APP_OTP_RESEND_COOLDOWN_SECONDS` (default `60`)
- `VITE_API_URL` (frontend)

## Demo walkthrough (capstone review, ~5 min)
1. Boot backend + open `/services` (public catalog).
2. Register a customer (name + email + password) → read the 6-digit code from the inbox → verify → auto-logged in to `/customer`; book a service for tomorrow → note the queue number.
3. Log out, log back in → email + password → session (unverified accounts get 403 + a fresh code).
4. Book the same barber/slot again → friendly "already booked" 409, selections kept.
5. Log in as admin (`ajanani416@gmail.com` / `Admin@123`) → dashboard counts → `/admin/queue` shows the wait math → move the appointment QUEUED→IN_SERVICE→COMPLETED (try skipping to COMPLETED: 409).
6. `/admin/services`: deactivate a service → it vanishes from public catalog; try deleting a referenced one → 409 with remedy.

## Known limitations (honest scope)
- No pagination on admin lists (fine at MVP scale; revisit with growth).
- Wait estimates are same-day, per-barber duration sums — no-shows/overruns aren't modeled.
- Queue order immutable by design; no WebSocket live updates (poll/refresh), no notifications beyond OTP, no payments/analytics (all listed as future scope in Problem_Statement).
- SMTP delivery requires operator Gmail App Password config; without it registration answers 502 (safe retry).
- Dev admin password is committed; rotate before any shared demo. No password reset flow (out of MVP scope — login page has no "forgot password" link by design).
