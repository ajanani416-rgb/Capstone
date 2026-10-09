package com.salon.management.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Skips the PostgreSQL-backed flow test when no local PG answers on 5432
 * or when the salon_management database is not reachable. Runs before Spring
 * boots anything, so the suite stays green on machines without a database
 * while proving the real-PG path where one exists. */
class RequiresLocalPostgresCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 2000);
        } catch (IOException e) {
            return ConditionEvaluationResult.disabled("no local PostgreSQL on 5432");
        }
        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:postgresql://localhost:5432/salon_management");
        String user = System.getenv().getOrDefault("DB_USERNAME", "postgres");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "password");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            return ConditionEvaluationResult.enabled("local PostgreSQL reachable");
        } catch (SQLException e) {
            return ConditionEvaluationResult.disabled(
                    "salon_management database not reachable: " + e.getMessage());
        }
    }
}
