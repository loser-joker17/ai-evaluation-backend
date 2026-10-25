package com.aievaluation.backend.service;

import java.time.Instant;
import java.util.UUID;

import com.aievaluation.backend.client.dto.EvaluatorJobResponse;
import com.aievaluation.backend.enums.ApplyOutcome;

public interface EvaluationResultService {

    /** Applies the evaluator's view of a job to our row. Safe to call more than once. */
    ApplyOutcome apply(UUID evaluationId, EvaluatorJobResponse job);
    /** The evaluator doesn't know this job (404). Resend with the same id, or FAILED LOST after 3 times. */
    ApplyOutcome markLost(UUID evaluationId);

    /** FAILED TIMEOUT if the job is still in flight and was queued before the cutoff. */
    ApplyOutcome failIfQueuedBefore(UUID evaluationId, Instant cutoff);
}