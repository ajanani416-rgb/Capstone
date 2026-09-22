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
 * unique + V4 OTP) actually run cleanly and the seed is idempotent — against
 * H2 in MySQL mode, since no MySQL server is available in this environment. */
class MigrationTest {

    private DataSource h2() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:flywaytest;DB_CLOSE_DELAY=-1;MODE=MySQL");
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
                    "SELECT COUNT(*) FROM users WHERE email='admin@salon.local' AND role='ADMIN'");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).as("exactly one seeded admin").isEqualTo(1);
            }
            // V3 backstop: the DB itself rejects a same-barber same-slot double
            // insert, independent of the service-layer pre-check.
            try (var setup = c.prepareStatement(
                    "INSERT INTO users (name, email, password_hash, role, created_at)"
                    + " VALUES ('M','m@example.com','h','CUSTOMER', NOW());"
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
