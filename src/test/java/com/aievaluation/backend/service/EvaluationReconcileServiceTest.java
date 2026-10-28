package com.aievaluation.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.IntegrationTestBase;
import com.aievaluation.backend.TestData;
import com.aievaluation.backend.client.EvaluatorClient;
import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.client.dto.JobLookup;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationErrorCode;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.repository.SubmissionRepository;
import com.aievaluation.backend.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@Transactional
class EvaluationReconcileServiceTest extends IntegrationTestBase {

    // Replaces the real HTTP client: each test decides what the "evaluator" answers.
    @MockitoBean
    private EvaluatorClient evaluatorClient;

    private final EvaluationReconcileService reconcileService;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;
    private final TestData testData;

    EvaluationReconcileServiceTest(EvaluationReconcileService reconcileService,
                                   EvaluationRepository evaluationRepository, ObjectMapper objectMapper,
                                   UserRepository userRepository, SubmissionRepository submissionRepository,
                                   EvaluationService evaluationService) {
        this.reconcileService = reconcileService;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
        this.testData = new TestData(userRepository, submissionRepository, evaluationService);
    }

    @Test
    void lostJob_isSentAgainWithSameId() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(5), 0);
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.notFound());

        reconcileService.reconcileBatch();

        Evaluation saved = reload(evaluation);
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.PENDING);
        assertThat(saved.getLostCount()).isEqualTo(1);
        assertThat(saved.getNextDispatchAt()).isNotNull();   // the dispatcher picks it up again
        assertThat(saved.getAttemptNo()).isEqualTo(1);        // same attempt, same id
    }

    @Test
    void lostThreeTimesAlready_failsAsLost() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(5), 3);
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.notFound());

        reconcileService.reconcileBatch();

        Evaluation saved = reload(evaluation);
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo(EvaluationErrorCode.LOST);
    }

    @Test
    void missedCallback_resultIsFetchedAndApplied() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(5), 0);
        EvaluatorJobResponse job = new EvaluatorJobResponse();
        job.setStatus(EvaluatorJobResponse.STATUS_DONE);
        job.setResult(objectMapper.readTree("{\"status\": \"success\", \"total_score\": 6.0}"));
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.found(job));

        reconcileService.reconcileBatch();

        assertThat(reload(evaluation).getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    void stillRunningAfter45Minutes_timesOut() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(50), 0);
        EvaluatorJobResponse job = new EvaluatorJobResponse();
        job.setStatus(EvaluatorJobResponse.STATUS_RUNNING);
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.found(job));

        reconcileService.reconcileBatch();

        Evaluation saved = reload(evaluation);
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.FAILED);
        assertThat(saved.getErrorCode()).isEqualTo(EvaluationErrorCode.TIMEOUT);
    }

    @Test
    void evaluatorDown_isNotTreatedAsLost() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(5), 0);
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.unavailable());

        reconcileService.reconcileBatch();

        Evaluation saved = reload(evaluation);
        assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.QUEUED);   // just wait
        assertThat(saved.getLostCount()).isEqualTo(0);                       // never resent
    }

    @Test
    void evaluatorDownPast45Minutes_stillTimesOut() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(50), 0);
        when(evaluatorClient.getJob(evaluation.getId())).thenReturn(JobLookup.unavailable());

        reconcileService.reconcileBatch();

        assertThat(reload(evaluation).getErrorCode()).isEqualTo(EvaluationErrorCode.TIMEOUT);
    }

    @Test
    void recentlyCheckedJob_isNotAskedAgain() {
        Evaluation evaluation = inFlight(Duration.ofMinutes(1), 0);
        evaluation.setLastCheckedAt(Instant.now());   // checked just now: not stale yet
        evaluationRepository.saveAndFlush(evaluation);

        reconcileService.reconcileBatch();

        verify(evaluatorClient, never()).getJob(any());
    }

    // ------------------------------------------------------------------ helpers

    /** A QUEUED job, queued `queuedAgo` ago, last checked long enough ago to be stale. */
    private Evaluation inFlight(Duration queuedAgo, int lostCount) {
        Evaluation evaluation = testData.firstAttempt();
        Instant queuedAt = Instant.now().minus(queuedAgo);
        evaluation.setStatus(EvaluationStatus.QUEUED);
        evaluation.setQueuedAt(queuedAt);
        evaluation.setLastCheckedAt(Instant.now().minus(Duration.ofMinutes(3)));
        evaluation.setNextDispatchAt(null);
        evaluation.setLostCount(lostCount);
        return evaluationRepository.saveAndFlush(evaluation);
    }

    private Evaluation reload(Evaluation evaluation) {
        return evaluationRepository.findById(evaluation.getId()).orElseThrow();
    }
}