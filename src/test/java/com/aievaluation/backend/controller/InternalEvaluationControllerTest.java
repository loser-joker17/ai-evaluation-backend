package com.aievaluation.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.IntegrationTestBase;
import com.aievaluation.backend.TestData;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.repository.SubmissionRepository;
import com.aievaluation.backend.repository.UserRepository;
import com.aievaluation.backend.service.EvaluationService;

@AutoConfigureMockMvc
@Transactional   // MockMvc runs in the test's thread, so the request's writes are rolled back too
class InternalEvaluationControllerTest extends IntegrationTestBase {

    private static final String SUCCESS_BODY = """
            {"job_id": "%s", "status": "done", "started_at": 1761388000.5, "finished_at": 1761388205.2,
             "result": {"status": "success", "total_score": 4.75, "feedback_consistent": true,
                        "run_info": {"evaluator_version": "test-1"}}}
            """;

    private final MockMvc mockMvc;
    private final EvaluationRepository evaluationRepository;
    private final TestData testData;

    InternalEvaluationControllerTest(MockMvc mockMvc, EvaluationRepository evaluationRepository,
                                     UserRepository userRepository, SubmissionRepository submissionRepository,
                                     EvaluationService evaluationService) {
        this.mockMvc = mockMvc;
        this.evaluationRepository = evaluationRepository;
        this.testData = new TestData(userRepository, submissionRepository, evaluationService);
    }

    @Test
    void wrongKey_is401InErrorFormat_andChangesNothing() throws Exception {
        Evaluation evaluation = testData.firstAttempt();

        callback(evaluation.getId().toString(), "wrong-key", SUCCESS_BODY.formatted(evaluation.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());

        assertThat(statusOf(evaluation)).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    void missingKey_is401() throws Exception {
        Evaluation evaluation = testData.firstAttempt();

        mockMvc.perform(post("/internal/evaluations/{id}", evaluation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUCCESS_BODY.formatted(evaluation.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void success_is204_andCompletes() throws Exception {
        Evaluation evaluation = testData.firstAttempt();

        callback(evaluation.getId().toString(), INTERNAL_KEY, SUCCESS_BODY.formatted(evaluation.getId()))
                .andExpect(status().isNoContent());

        assertThat(statusOf(evaluation)).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    void duplicateCallback_is204Again() throws Exception {
        Evaluation evaluation = testData.firstAttempt();
        String body = SUCCESS_BODY.formatted(evaluation.getId());

        callback(evaluation.getId().toString(), INTERNAL_KEY, body).andExpect(status().isNoContent());
        // The evaluator retries on errors, so a repeat must be a quiet success, not an error.
        callback(evaluation.getId().toString(), INTERNAL_KEY, body).andExpect(status().isNoContent());

        assertThat(statusOf(evaluation)).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    void unknownJob_is404() throws Exception {
        UUID unknown = UUID.randomUUID();

        callback(unknown.toString(), INTERNAL_KEY, SUCCESS_BODY.formatted(unknown))
                .andExpect(status().isNotFound());
    }

    @Test
    void idThatIsNotUuid_is404() throws Exception {
        callback("not-a-uuid", INTERNAL_KEY, "{\"status\": \"done\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void bodyJobIdMismatch_is400_andChangesNothing() throws Exception {
        Evaluation evaluation = testData.firstAttempt();

        callback(evaluation.getId().toString(), INTERNAL_KEY, SUCCESS_BODY.formatted(UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        assertThat(statusOf(evaluation)).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    void malformedJson_is400() throws Exception {
        Evaluation evaluation = testData.firstAttempt();

        callback(evaluation.getId().toString(), INTERNAL_KEY, "{not json")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownExtraFields_areIgnored() throws Exception {
        Evaluation evaluation = testData.firstAttempt();
        String body = """
                {"job_id": "%s", "status": "done", "some_new_field": 42,
                 "result": {"status": "success", "total_score": 5.0, "brand_new": [1, 2]}}
                """.formatted(evaluation.getId());

        // A newer evaluator adding fields must never break callbacks (a 400 would stop its retries).
        callback(evaluation.getId().toString(), INTERNAL_KEY, body).andExpect(status().isNoContent());

        assertThat(statusOf(evaluation)).isEqualTo(EvaluationStatus.COMPLETED);
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions callback(String id, String key, String body) throws Exception {
        return mockMvc.perform(post("/internal/evaluations/{id}", id)
                .header("X-Internal-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private EvaluationStatus statusOf(Evaluation evaluation) {
        return evaluationRepository.findById(evaluation.getId()).orElseThrow().getStatus();
    }
}