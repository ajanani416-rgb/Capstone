-- V3: D2 exact-slot block. One barber cannot hold two appointments at the
-- same date+time. The service layer pre-checks and returns a friendly 409;
-- this constraint is the race-proof backstop (violations surface as 409 too).
ALTER TABLE appointments
    ADD CONSTRAINT uq_appointments_barber_slot
    UNIQUE (barber_id, appointment_date, appointment_time);
