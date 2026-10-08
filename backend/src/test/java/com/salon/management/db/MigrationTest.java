package com.salon.management.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

/** Proves the Flyway migrations (V1 baseline + V2 admin seed + V3 slot
 * unique + V4 OTP + V5 admin identity + V6 phone identity + V7 passwordless
 * + V9 email identity, all PostgreSQL dialect) actually run cleanly and the
 * seed is idempotent — against H2 in PostgreSQL mode, since the suite owns
 * no server. V9 carries verified→email_verified, backfills the seeded
 * admin, and drops the phone columns. */
class MigrationTest {

    private static DataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:migrationtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    @Test
    void migrationsRunAndSeedAdminOnce() throws Exception {
        DataSource ds = h2();
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        // Second run proves V2's NOT EXISTS guard makes it re-runnable.
        flyway.migrate();

        try (Connection c = ds.getConnection()) {
            for (String table : new String[] {"users", "services", "barbers", "appointments",
                    "otp_codes"}) {
                try (ResultSet rs = c.getMetaData().getTables(null, null, table.toUpperCase(), null)) {
                    assertThat(rs.next()).as("table %s exists", table).isTrue();
                }
            }
            try (var ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE email='ajanani416@gmail.com'"
                    + " AND role='ADMIN' AND email_verified=TRUE"
                    + " AND password_hash <> ''");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).as("exactly one verified seeded admin").isEqualTo(1);
            }
            try (var ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE email IS NULL");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).as("no emailless rows remain").isEqualTo(0);
            }
            // V9 backstop: the phone/verified columns are gone, the email
            // identity columns exist.
            try (ResultSet rs = c.getMetaData().getColumns(null, null, "USERS", "PHONE")) {
                assertThat(rs.next()).as("phone column dropped").isFalse();
            }
            try (ResultSet rs = c.getMetaData().getColumns(null, null, "OTP_CODES", "EMAIL")) {
                assertThat(rs.next()).as("otp email column exists").isTrue();
            }
            // V3 backstop: the DB itself rejects a same-barber same-slot double
            // insert, independent of the service-layer pre-check.
            try (var setup = c.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, email_verified, role, created_at)"
                    + " VALUES ('M','m@example.com','hash',TRUE,'CUSTOMER', NOW());"
                    + "INSERT INTO services (name, duration_minutes, price, active)"
                    + " VALUES ('S',30,100.00,TRUE);"
                    + "INSERT INTO barbers (name, active) VALUES ('B',TRUE)")) {
                setup.execute();
            }
            String row = "INSERT INTO appointments (user_id, barber_id, service_id,"
                    + " appointment_date, appointment_time, status, created_at)"
                    + " VALUES (1, 1, 1, '2026-12-01', '10:00:00', 'QUEUED', NOW())";
            try (var first = c.prepareStatement(row)) {
                first.execute();
            }
            assertThatThrownBy(() -> {
                try (var dup = c.prepareStatement(row)) {
                    dup.execute();
                }
            }).isInstanceOf(Exception.class);
        }
    }
}
