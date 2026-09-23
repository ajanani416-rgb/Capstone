-- V6: phone replaces email as the account identity and the OTP channel
-- (SMS verification). V1/V2/V5 stay frozen as history.
-- (PostgreSQL dialect per TASK-013; same H2 note as V1.)
--
-- Demo admin phone below is a DEV-ONLY placeholder (obviously fake 000000
-- pattern): change the number immediately in any shared environment.
--
-- Assumption (documented): pre-V6 databases only carry the seeded admin row
-- plus API-created demo users. The admin row is backfilled here; any other
-- pre-existing rows keep phone NULL and must re-register (acceptable for a
-- disposable demo DB; production would need a real data-migration script).
ALTER TABLE users ADD COLUMN phone VARCHAR(20) NULL;
UPDATE users SET phone = '+910000000001' WHERE email = 'ajanani416@gmail.com';
ALTER TABLE users ADD CONSTRAINT uq_users_phone UNIQUE (phone);
ALTER TABLE users DROP COLUMN email;

-- Old email-keyed codes are disposable (10-minute TTL): carry the value over
-- so the table stays consistent, then drop the email column.
ALTER TABLE otp_codes ADD COLUMN phone VARCHAR(20) NULL;
UPDATE otp_codes SET phone = email;
DROP INDEX idx_otp_email_purpose;
ALTER TABLE otp_codes DROP COLUMN email;
CREATE INDEX idx_otp_phone_purpose ON otp_codes (phone, purpose);
