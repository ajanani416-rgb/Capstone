-- V5: admin identity → ajanani416@gmail.com (operator decision: the salon
-- owner is the admin). V2 stays frozen as history; this rename is idempotent
-- (second run matches zero rows). Password unchanged (see V2 header).
UPDATE users SET email = 'ajanani416@gmail.com' WHERE email = 'admin@salon.local';
