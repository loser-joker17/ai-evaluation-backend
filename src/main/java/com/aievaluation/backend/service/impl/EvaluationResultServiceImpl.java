package com.aievaluation.backend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.ApplyOutcome;
import com.aievaluation.backend.enums.EvaluationErrorCode;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.EvaluationResultService;
import com.aievaluation.backend.service.EvaluationService;

import tools.jackson.databind.JsonNode;

@Service
public class EvaluationResultServiceImpl implements EvaluationResultService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationResultServiceImpl.class);

    private static final int MAX_ERROR_LENGTH = 500;
    private static final int MAX_LOST = 3;
    // Only attempt 1 gets an automatic retry; a failed retry stays FAILED.
    private static final int AUTO_RETRY_ONLY_FOR_ATTEMPT = 1;

    private final EvaluationRepository evaluationRepository;
    private final EvaluationService evaluationService;

    public EvaluationResultServiceImpl(EvaluationRepository evaluationRepository,
                                       EvaluationService evaluationService) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationService = evaluationService;
    }

    @Override
    @Transactional
    public ApplyOutcome apply(UUID evaluationId, EvaluatorJobResponse job) {
        Optional<Evaluation> found = evaluationRepository.findByIdForUpdate(evaluationId);
        if (found.isEmpty()) {
            return ApplyOutcome.NOT_FOUND;
        }
        Evaluation evaluation = found.get();

        // Idempotent: a final status is never overwritten.
        if (evaluation.getStatus().isFinal()) {
            return ApplyOutcome.ALREADY_FINAL;
        }

        Instant now = Instant.now();
        evaluation.setLastCheckedAt(now);
        evaluation.setNextDispatchAt(null);
        if (job.getStartedAt() != null) {
            evaluation.setStartedAt(toInstant(job.getStartedAt()));
        }

        String status = job.getStatus();
        if (EvaluatorJobResponse.STATUS_DONE.equals(status)) {
            applyDone(evaluation, job, now);
            return ApplyOutcome.APPLIED;
        }
        if (EvaluatorJobResponse.STATUS_FAILED.equals(status)) {
            applyFailed(evaluation, job, now);
            return ApplyOutcome.APPLIED;
        }

        // queued / running (only the reconciler sees these). Callback only sends final states.
        if (EvaluatorJobResponse.STATUS_RUNNING.equals(status)) {
            evaluation.setStatus(EvaluationStatus.RUNNING);
        } else if (evaluation.getStatus() == EvaluationStatus.PENDING) {
            // The evaluator has it, but the dispatcher hasn't recorded that yet.
            evaluation.setStatus(EvaluationStatus.QUEUED);
            evaluation.setQueuedAt(now);
        }
        return ApplyOutcome.STILL_RUNNING;
    }

    private void applyDone(Evaluation evaluation, EvaluatorJobResponse job, Instant now) {
        JsonNode result = job.getResult();
        evaluation.setFinishedAt(finishedAt(job, now));

        if (result == null || result.isNull()) {
            // "done" without a result is a broken contract; treat it as an evaluator failure.
            markFailed(evaluation, EvaluationErrorCode.EVALUATOR_FAILED, "Evaluator returned no result");
            scheduleRetryIfAllowed(evaluation);
            return;
        }

        evaluation.setResultJson(result.toString());
        JsonNode runInfo = result.path("run_info");
        if (runInfo.isObject()) {
            evaluation.setRunInfo(runInfo.toString());
            JsonNode version = runInfo.path("evaluator_version");
            if (!version.isMissingNode() && !version.isNull()) {
                evaluation.setEvaluatorVersion(truncate(version.asString(), 40));
            }
        }

        if ("success".equals(result.path("status").asString())) {
            JsonNode score = result.path("total_score");
            if (score.isNumber()) {
                evaluation.setTotalScore(score.decimalValue().setScale(2, java.math.RoundingMode.HALF_UP));
            }
            JsonNode consistent = result.path("feedback_consistent");
            if (consistent.isBoolean()) {
                evaluation.setFeedbackConsistent(consistent.booleanValue());
            }
            evaluation.setStatus(EvaluationStatus.COMPLETED);
            log.info("Evaluation {} completed (attempt {})", evaluation.getId(), evaluation.getAttemptNo());
        } else {
            // The evaluator refused the input (e.g. not an answer). Reasons stay in result_json for step 7.
            evaluation.setStatus(EvaluationStatus.REJECTED);
            log.info("Evaluation {} rejected by the evaluator", evaluation.getId());
        }
    }

    @Override
    @Transactional
    public ApplyOutcome markLost(UUID evaluationId) {
        Optional<Evaluation> found = evaluationRepository.findByIdForUpdate(evaluationId);
        if (found.isEmpty()) {
            return ApplyOutcome.NOT_FOUND;
        }
        Evaluation evaluation = found.get();
        if (!isInFlight(evaluation)) {
            return ApplyOutcome.ALREADY_FINAL;   // a result arrived meanwhile
        }

        Instant now = Instant.now();
        evaluation.setLastCheckedAt(now);
        if (evaluation.getLostCount() < MAX_LOST) {
            // Same id: the evaluator has forgotten it, so the id is free to use again.
            evaluation.setLostCount(evaluation.getLostCount() + 1);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setNextDispatchAt(now);
            log.warn("Evaluation {} lost by the evaluator; sending again ({}/{})",
                    evaluationId, evaluation.getLostCount(), MAX_LOST);
        } else {
            markFailed(evaluation, EvaluationErrorCode.LOST, "The evaluator lost this job too many times");
            evaluation.setFinishedAt(now);
            log.error("Evaluation {} lost {} times; marked FAILED", evaluationId, MAX_LOST);
        }
        return ApplyOutcome.APPLIED;
    }

    @Override
    @Transactional
    public ApplyOutcome failIfQueuedBefore(UUID evaluationId, Instant cutoff) {
        Optional<Evaluation> found = evaluationRepository.findByIdForUpdate(evaluationId);
        if (found.isEmpty()) {
            return ApplyOutcome.NOT_FOUND;
        }
        Evaluation evaluation = found.get();
        if (!isInFlight(evaluation)) {
            return ApplyOutcome.ALREADY_FINAL;
        }
        Instant queuedAt = evaluation.getQueuedAt() != null ? evaluation.getQueuedAt() : evaluation.getCreatedAt();
        if (!queuedAt.isBefore(cutoff)) {
            return ApplyOutcome.STILL_RUNNING;
        }
        markFailed(evaluation, EvaluationErrorCode.TIMEOUT, "No result within the time limit");
        evaluation.setFinishedAt(Instant.now());
        log.warn("Evaluation {} timed out", evaluationId);
        return ApplyOutcome.APPLIED;
    }

    private static boolean isInFlight(Evaluation evaluation) {
        return evaluation.getStatus() == EvaluationStatus.QUEUED
                || evaluation.getStatus() == EvaluationStatus.RUNNING;
    }

    private void applyFailed(Evaluation evaluation, EvaluatorJobResponse job, Instant now) {
        evaluation.setFinishedAt(finishedAt(job, now));
        // The error text can contain answer text: store it, never log it.
        markFailed(evaluation, EvaluationErrorCode.EVALUATOR_FAILED, job.getError());
        log.warn("Evaluation {} failed at the evaluator (attempt {})", evaluation.getId(), evaluation.getAttemptNo());
        scheduleRetryIfAllowed(evaluation);
    }

    private void markFailed(Evaluation evaluation, EvaluationErrorCode code, String message) {
        evaluation.setStatus(EvaluationStatus.FAILED);
        evaluation.setErrorCode(code);
        evaluation.setErrorMessage(truncate(message, MAX_ERROR_LENGTH));
    }

    // New attempt with a new id: the evaluator keeps the failed id for an hour and would
    // just return the old failure if we sent the same id again.
    private void scheduleRetryIfAllowed(Evaluation evaluation) {
        if (evaluation.getAttemptNo() == AUTO_RETRY_ONLY_FOR_ATTEMPT) {
            Evaluation retry = evaluationService.createAttempt(evaluation.getSubmission());
            log.info("Evaluation {} will be retried as {} (attempt {})",
                    evaluation.getId(), retry.getId(), retry.getAttemptNo());
        }
    }

    private Instant finishedAt(EvaluatorJobResponse job, Instant fallback) {
        return job.getFinishedAt() == null ? fallback : toInstant(job.getFinishedAt());
    }

    private static Instant toInstant(double epochSeconds) {
        return Instant.ofEpochMilli(Math.round(epochSeconds * 1000));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}