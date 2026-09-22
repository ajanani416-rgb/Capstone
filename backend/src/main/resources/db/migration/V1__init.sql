-- V1 baseline. Faithful port of docs/database-schema.md (users, services,
-- barbers, appointments). Engine: InnoDB / utf8mb4. Flyway owns this schema;
-- Hibernate ddl-auto=validate must match it.
--
-- Deliberately NOT included (open decisions at the time, all since resolved):
--   D2 unique constraint lives in V3; D3 status values are enforced in code.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS services (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    active BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS barbers (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    specialization VARCHAR(255) NULL,
    active BOOLEAN NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    barber_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    queue_number INT NULL,
    status VARCHAR(50) NOT NULL,
    estimated_wait_minutes INT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_barber FOREIGN KEY (barber_id) REFERENCES barbers (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id),
    INDEX idx_appointments_user (user_id),
    INDEX idx_appointments_barber_slot (barber_id, appointment_date, appointment_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
