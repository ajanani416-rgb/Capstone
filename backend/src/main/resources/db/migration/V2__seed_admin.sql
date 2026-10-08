-- V2: seed the initial admin account (decision D4). Public registration can
-- only create CUSTOMER rows, so without this seed no admin could ever exist.
--
-- Credentials: admin@salon.local / Admin@123 (BCrypt hash below).
-- DEV-ONLY default: change the password immediately after first login in any
-- shared environment, and rotate it before the capstone demo if this value
-- has ever been committed or screenshared.
INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Salon Admin', 'admin@salon.local',
    '$2a$10$9FlmA3wmqJwpaoVn/6.t2e75m4/BoGhMeYAr3LGZgBCxG23ogCtgy',
    'ADMIN', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@salon.local');
