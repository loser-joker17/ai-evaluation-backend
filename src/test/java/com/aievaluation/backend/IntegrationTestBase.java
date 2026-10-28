package com.aievaluation.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for tests that need Spring and a database. One Postgres container is started
 * for the whole test run and shared by all test classes (faster than one per class).
 */
@SpringBootTest(properties = {
        "app.scheduling.enabled=false",
        "app.auth.jwt-secret=dGVzdC1qd3Qtc2VjcmV0LWZvci10ZXN0cy1vbmx5LTAxMjM0NTY3ODlhYmNkZWY=",
        "app.evaluator.internal-key=test-internal-key-0123456789-abcdefghij",
        // Tests never reach a real evaluator; B7.4 replaces the client, B7.5 uses WireMock.
        "app.evaluator.base-url=http://localhost:1"
})
public abstract class IntegrationTestBase {

    protected static final String INTERNAL_KEY = "test-internal-key-0123456789-abcdefghij";

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }
}