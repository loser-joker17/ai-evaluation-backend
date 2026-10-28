package com.aievaluation.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.IntegrationTestBase;
import com.aievaluation.backend.TestData;
import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.ApplyOutcome;
import com.aievaluation.backend.enums.EvaluationErrorCode;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.repository.SubmissionRepository;
import com.aievaluation.backend.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@Transactional   // every test is rolled back
class EvaluationResultServiceTest extends IntegrationTestBase {

    private static final String SUCCESS_RESULT = """
            {"status": "success", "total_score": 4.75, "feedback_consistent": true,
             "dimension_scores": [],
             "run_info": {"evaluator_version": "test-1", "seconds": 205}}
            """;

    private static final String ERROR_RESULT = """
            {"status": "error", "errors": ["Answer is empty"], "max_marks": 10}
            """;

    private final EvaluationResultService resultService;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final TestData testData;

    EvaluationResultServiceTest(EvaluationResultService resultService, EvaluationRepository evaluationRepository,
                                ObjectMapper objectMapper, UserRepository userRepository,
                                SubmissionRepository submissionRepository, EvaluationService evaluationService) {
        this.resultService = resultService;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
        this.testData = new TestData(userRepository, submissionRepository, evaluationService);
    }

    @Test
    void doneWithSuccess_completesAndStoresScore() {
        Evaluation evaluation = testData.firstAttempt();

        ApplyOutcome outcome = resultService.apply(evaluation.getId(), done(SUCCESS_RESULT));

        assertThat(outcome).isEqualTo(ApplyOutcome.APPLIED);
        Evaluation saved = evaluationRepository.findById(evaluation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(saved.getTotalScore()).isEqualByComparingTo(new BigDecimal("4.75"));
        assertThat(saved.getEvaluatorVersion()).isEqualTo("test-1");
        assertThat(saved.getFeedbackConsistent()).isTrue();
        assertThat(saved.getResultJson()).contains("total_score");
        assertThat(saved.getRunInfo()).contains("evaluator_version");
        assertThat(saved.getFinishedAt()).isNotNull();
    }

    @Test
    void doneWithError_isRejectedWithoutScore() {
        Evaluation evaluation = testData.firstAttempt();

        resultService.apply(evaluation.getId(), done(ERROR_RESULT));

        Evaluation saved = evaluationRepository.findById(evaluation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.REJECTED);
        assertThat(saved.getTotalScore()).isNull();
        assertThat(saved.getResultJson()).contains("Answer is empty");
    }

    @Test
    void failedOnFirstAttempt_createsAutomaticRetry() {
        Evaluation evaluation = testData.firstAttempt();
        UUID submissionId = evaluation.getSubmission().getId();

        resultService.apply(evaluation.getId(), failed());

        Evaluation first = evaluationRepository.findById(evaluation.getId()).orElseThrow();
        assertThat(first.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(first.getErrorCode()).isEqualTo(EvaluationErrorCode.EVALUATOR_FAILED);

        Evaluation latest = evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submissionId).orElseThrow();
        assertThat(latest.getAttemptNo()).isEqualTo(2);
        assertThat(latest.getStatus()).isEqualTo(EvaluationStatus.PENDING);
        assertThat(latest.getId()).isNotEqualTo(evaluation.getId());   // new job id
    }

    @Test
    void failedOnSecondAttempt_doesNotRetryAgain() {
        Evaluation first = testData.firstAttempt();
        UUID submissionId = first.getSubmission().getId();
        resultService.apply(first.getId(), failed());
        Evaluation second = evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submissionId).orElseThrow();

        resultService.apply(second.getId(), failed());

        Evaluation latest = evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submissionId).orElseThrow();
        assertThat(latest.getAttemptNo()).isEqualTo(2);
        assertThat(latest.getStatus()).isEqualTo(EvaluationStatus.FAILED);
    }

    @Test
    void finalStatus_isNeverOverwritten() {
        Evaluation evaluation = testData.firstAttempt();
        resultService.apply(evaluation.getId(), done(SUCCESS_RESULT));

        // A late or duplicate message must change nothing, and must not start a retry.
        ApplyOutcome second = resultService.apply(evaluation.getId(), failed());

        assertThat(second).isEqualTo(ApplyOutcome.ALREADY_FINAL);
        Evaluation saved = evaluationRepository.findById(evaluation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        Evaluation latest = evaluationRepository
                .findFirstBySubmissionIdOrderByAttemptNoDesc(evaluation.getSubmission().getId()).orElseThrow();
        assertThat(latest.getAttemptNo()).isEqualTo(1);
    }

    @Test
    void running_marksRunningAndKeepsWaiting() {
        Evaluation evaluation = testData.firstAttempt();

        ApplyOutcome outcome = resultService.apply(evaluation.getId(), job(EvaluatorJobResponse.STATUS_RUNNING));

        assertThat(outcome).isEqualTo(ApplyOutcome.STILL_RUNNING);
        Evaluation saved = evaluationRepository.findById(evaluation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.RUNNING);
        assertThat(saved.getLastCheckedAt()).isNotNull();
    }

    @Test
    void unknownId_isNotFound() {
        ApplyOutcome outcome = resultService.apply(UUID.randomUUID(), done(SUCCESS_RESULT));

        assertThat(outcome).isEqualTo(ApplyOutcome.NOT_FOUND);
    }

    // ------------------------------------------------------------------ helpers

    private EvaluatorJobResponse job(String status) {
        EvaluatorJobResponse job = new EvaluatorJobResponse();
        job.setStatus(status);
        job.setStartedAt(1761388000.5);
        return job;
    }

    private EvaluatorJobResponse done(String resultJson) {
        EvaluatorJobResponse job = job(EvaluatorJobResponse.STATUS_DONE);
        job.setFinishedAt(1761388205.2);
        job.setResult(objectMapper.readTree(resultJson));
        return job;
    }

    private EvaluatorJobResponse failed() {
        EvaluatorJobResponse job = job(EvaluatorJobResponse.STATUS_FAILED);
        job.setError("TimeoutError: test");
        return job;
    }
}