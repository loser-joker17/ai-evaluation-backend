package com.aievaluation.backend.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.aievaluation.backend.client.enums.DispatchOutcome;
import com.aievaluation.backend.client.EvaluatorClient;
import com.aievaluation.backend.client.dto.EvaluatorInput;
import com.aievaluation.backend.config.EvaluatorProperties;
import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.enums.EvaluationErrorCode;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationDispatchRepository;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.EvaluationDispatchService;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class EvaluationDispatchServiceImpl implements EvaluationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationDispatchServiceImpl.class);

    private static final Duration LEASE = Duration.ofMinutes(2);
    private static final Duration BUSY_RETRY = Duration.ofSeconds(60);
    private static final Duration FIRST_RETRY = Duration.ofSeconds(30);
    private static final Duration MAX_RETRY = Duration.ofMinutes(5);

    private final EvaluationDispatchRepository dispatchRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluatorClient evaluatorClient;
    private final EvaluatorProperties evaluatorProperties;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    public EvaluationDispatchServiceImpl(EvaluationDispatchRepository dispatchRepository,
                                         EvaluationRepository evaluationRepository,
                                         EvaluatorClient evaluatorClient,
                                         EvaluatorProperties evaluatorProperties,
                                         TransactionTemplate transactionTemplate,
                                         MeterRegistry meterRegistry) {
        this.dispatchRepository = dispatchRepository;
        this.evaluationRepository = evaluationRepository;
        this.evaluatorClient = evaluatorClient;
        this.evaluatorProperties = evaluatorProperties;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int dispatchDueBatch() {
        Instant now = Instant.now();
        List<UUID> ids = dispatchRepository.claimDue(now, now.plus(LEASE),
                evaluatorProperties.getDispatchBatchSize());
        for (UUID id : ids) {
            dispatchOne(id);
        }
        return ids.size();
    }

    private void dispatchOne(UUID evaluationId) {
        // 1. Read what to send (short read-only transaction).
        EvaluatorInput input = transactionTemplate.execute(status -> buildInput(evaluationId));
        if (input == null) {
            return;   // deleted or no longer PENDING
        }

        // 2. Call the evaluator outside any transaction.
        DispatchOutcome outcome = evaluatorClient.submitJob(evaluationId, input);
        meterRegistry.counter("evaluator.dispatch", "outcome", outcome.name()).increment();

        // 3. Record the outcome (short write transaction).
        transactionTemplate.executeWithoutResult(status -> recordOutcome(evaluationId, outcome));
    }

    private EvaluatorInput buildInput(UUID evaluationId) {
        Optional<Evaluation> found = evaluationRepository.findById(evaluationId);
        if (found.isEmpty() || found.get().getStatus() != EvaluationStatus.PENDING) {
            return null;
        }
        Submission submission = found.get().getSubmission();
        EvaluatorInput input = new EvaluatorInput();
        input.setQuestion(submission.getQuestionText());
        input.setAnswer(submission.getAnswerText());
        input.setMaxMarks(submission.getMaxMarks());
        input.setWordLimit(submission.getWordLimit());
        input.setSubject(submission.getPaper().name());   // "GS4" turns on the ethics guidance
        return input;
    }

    private void recordOutcome(UUID evaluationId, DispatchOutcome outcome) {
        Optional<Evaluation> found = evaluationRepository.findByIdForUpdate(evaluationId);
        // Only change rows still PENDING. A very fast job's result (step 6) may already be in.
        if (found.isEmpty() || found.get().getStatus() != EvaluationStatus.PENDING) {
            return;
        }
        Evaluation evaluation = found.get();
        Instant now = Instant.now();

        if (outcome == DispatchOutcome.ACCEPTED) {
            evaluation.setStatus(EvaluationStatus.QUEUED);
            evaluation.setQueuedAt(now);
            evaluation.setLastCheckedAt(now);
            evaluation.setNextDispatchAt(null);
            log.info("Evaluation {} queued at the evaluator (attempt {})", evaluationId, evaluation.getAttemptNo());
        } else if (outcome == DispatchOutcome.BUSY) {
            evaluation.setNextDispatchAt(now.plus(BUSY_RETRY));
        } else if (outcome == DispatchOutcome.UNAVAILABLE) {
            evaluation.setNextDispatchAt(now.plus(backoff(evaluation.getDispatchCount())));
        } else {
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorCode(EvaluationErrorCode.CONTRACT);
            evaluation.setErrorMessage("The evaluator refused the request. Check EVALUATOR_INTERNAL_KEY and the input limits.");
            evaluation.setFinishedAt(now);
            evaluation.setNextDispatchAt(null);
        }
    }

    // 30 s, 60 s, 120 s, 240 s, then 5 min.
    private Duration backoff(int dispatchCount) {
        Duration wait = FIRST_RETRY;
        for (int i = 1; i < dispatchCount && wait.compareTo(MAX_RETRY) < 0; i++) {
            wait = wait.multipliedBy(2);
        }
        return wait.compareTo(MAX_RETRY) > 0 ? MAX_RETRY : wait;
    }
}