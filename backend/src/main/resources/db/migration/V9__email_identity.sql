-- V9: email + password identity (email OTP via SMTP). V1/V2/V5/V6/V7 stay
-- frozen as history; the uncommitted V8 phone placeholder never ran anywhere
-- shared and is superseded by this migration.
--
-- Identity transition: email becomes the login identity (nullable UNIQUE so
-- pre-V9 phone-only demo rows survive — MySQL and H2 both allow multiple
-- NULLs in a unique column; those rows cannot authenticate until they
-- re-register). password_hash is restored NOT NULL with an empty marker that
-- matches no BCrypt check. verified is carried into email_verified, then both
-- legacy columns are dropped. Dead phone-keyed OTP rows are dropped with
-- their column (codes live 10 minutes; none can be valid across a deploy).
--
-- Admin backfill: the single seeded ADMIN row (V2, renamed by V5, phoned by
-- V6) gets the operator email, the V2 BCrypt hash (Admin@123, DEV-ONLY per
-- the V2 header — rotate before any shared demo), and verified=true. Never
-- put a real Gmail/app password in a migration.
ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = verified;
UPDATE users SET email = 'ajanani416@gmail.com',
    password_hash = '$2a$10$9FlmA3wmqJwpaoVn/6.t2e75m4/BoGhMeYAr3LGZgBCxG23ogCtgy',
    email_verified = TRUE WHERE role = 'ADMIN';
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
ALTER TABLE users DROP COLUMN verified;
ALTER TABLE users DROP COLUMN phone;

ALTER TABLE otp_codes ADD COLUMN email VARCHAR(255) NULL;
DROP INDEX idx_otp_phone_purpose ON otp_codes;
ALTER TABLE otp_codes DROP COLUMN phone;
CREATE INDEX idx_otp_email_purpose ON otp_codes (email, purpose);
