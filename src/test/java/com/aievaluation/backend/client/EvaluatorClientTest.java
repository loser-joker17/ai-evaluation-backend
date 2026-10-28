package com.aievaluation.backend.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aievaluation.backend.client.dto.EvaluatorInput;
import com.aievaluation.backend.client.dto.JobLookup;
import com.aievaluation.backend.client.enums.DispatchOutcome;
import com.aievaluation.backend.client.enums.JobLookupStatus;
import com.aievaluation.backend.config.EvaluatorClientConfig;
import com.aievaluation.backend.config.EvaluatorProperties;
import com.sun.net.httpserver.HttpServer;

/** The real client and RestClient config against a tiny local HTTP server. No Spring, no Docker. */
class EvaluatorClientTest {

    private static final String KEY = "client-test-key";

    private HttpServer server;
    private EvaluatorClient client;

    // What the fake evaluator answers, and what it last received.
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile String receivedKey;
    private volatile String receivedPath;

    @BeforeEach
    void startServer() throws IOException {
        responseStatus = 202;
        responseBody = "";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedKey = exchange.getRequestHeaders().getFirst("X-Internal-Key");
            receivedPath = exchange.getRequestURI().getPath();
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
        client = clientFor(server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    // ------------------------------------------------------------------ POST /jobs

    @Test
    void submit_202_isAccepted_andSendsKey() {
        responseStatus = 202;

        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.ACCEPTED);
        assertThat(receivedKey).isEqualTo(KEY);
        assertThat(receivedPath).isEqualTo("/jobs");
    }

    @Test
    void submit_200_alreadyKnown_isAccepted() {
        responseStatus = 200;

        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.ACCEPTED);
    }

    @Test
    void submit_503_isBusy() {
        responseStatus = 503;

        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.BUSY);
    }

    @Test
    void submit_401_and_422_areRejected() {
        responseStatus = 401;
        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.REJECTED);

        responseStatus = 422;
        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.REJECTED);
    }

    @Test
    void submit_500_isUnavailable() {
        responseStatus = 500;

        assertThat(client.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.UNAVAILABLE);
    }

    @Test
    void submit_evaluatorDown_isUnavailable() {
        EvaluatorClient downClient = clientFor(closedPort());

        assertThat(downClient.submitJob(UUID.randomUUID(), input())).isEqualTo(DispatchOutcome.UNAVAILABLE);
    }

    // ------------------------------------------------------------------ GET /jobs/{id}

    @Test
    void getJob_200_isFound_withBody() {
        UUID id = UUID.randomUUID();
        responseStatus = 200;
        responseBody = """
                {"job_id": "%s", "status": "done", "result": {"status": "success", "total_score": 5.5},
                 "finished_at": 1761388205.2, "unknown_field": true}
                """.formatted(id);

        JobLookup lookup = client.getJob(id);

        assertThat(lookup.getStatus()).isEqualTo(JobLookupStatus.FOUND);
        assertThat(lookup.getJob().getStatus()).isEqualTo("done");
        assertThat(lookup.getJob().getResult().path("total_score").asDouble()).isEqualTo(5.5);
        assertThat(receivedPath).isEqualTo("/jobs/" + id);
    }

    @Test
    void getJob_404_isNotFound() {
        responseStatus = 404;
        responseBody = "{\"detail\": \"Unknown job\"}";

        assertThat(client.getJob(UUID.randomUUID()).getStatus()).isEqualTo(JobLookupStatus.NOT_FOUND);
    }

    @Test
    void getJob_401_isUnavailable_notLost() {
        // A wrong key must never look like "job lost", or every job would be resent and marked LOST.
        responseStatus = 401;

        assertThat(client.getJob(UUID.randomUUID()).getStatus()).isEqualTo(JobLookupStatus.UNAVAILABLE);
    }

    @Test
    void getJob_badJson_isUnavailable() {
        responseStatus = 200;
        responseBody = "{this is not json";

        assertThat(client.getJob(UUID.randomUUID()).getStatus()).isEqualTo(JobLookupStatus.UNAVAILABLE);
    }

    @Test
    void getJob_evaluatorDown_isUnavailable() {
        EvaluatorClient downClient = clientFor(closedPort());

        assertThat(downClient.getJob(UUID.randomUUID()).getStatus()).isEqualTo(JobLookupStatus.UNAVAILABLE);
    }

    // ------------------------------------------------------------------ helpers

    private EvaluatorClient clientFor(int port) {
        EvaluatorProperties properties = new EvaluatorProperties();
        properties.setBaseUrl("http://127.0.0.1:" + port);
        properties.setInternalKey(KEY);
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        return new EvaluatorClient(new EvaluatorClientConfig().evaluatorRestClient(properties));
    }

    /** A port where nothing listens: start a server, note its port, stop it. */
    private static int closedPort() {
        try {
            HttpServer temp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = temp.getAddress().getPort();
            temp.stop(0);
            return port;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static EvaluatorInput input() {
        EvaluatorInput input = new EvaluatorInput();
        input.setQuestion("Test question");
        input.setAnswer("Test answer");
        input.setMaxMarks(10);
        input.setWordLimit(150);
        input.setSubject("GS2");
        return input;
    }
}