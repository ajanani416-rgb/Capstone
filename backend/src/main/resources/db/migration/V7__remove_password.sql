-- V7: passwordless auth (phone + SMS OTP only). Identity is proven purely
-- via OTP challenge, so password hashes are dropped — all rows, including
-- the seeded admin. V1/V2 stay frozen as history.
ALTER TABLE users DROP COLUMN password_hash;
