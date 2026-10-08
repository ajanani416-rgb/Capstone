package com.salon.management.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Skips the PostgreSQL-backed flow test when no local PG answers on 5432.
 * Runs before Spring boots anything, so the suite stays green on machines
 * without a database while proving the real-PG path where one exists. */
class RequiresLocalPostgresCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 2000);
            return ConditionEvaluationResult.enabled("local PostgreSQL reachable");
        } catch (IOException e) {
            return ConditionEvaluationResult.disabled("no local PostgreSQL on 5432");
        }
    }
}
