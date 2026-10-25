package com.aievaluation.backend.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.aievaluation.backend.client.EvaluatorClient;
import com.aievaluation.backend.client.dto.JobLookup;
import com.aievaluation.backend.client.enums.JobLookupStatus;
import com.aievaluation.backend.enums.ApplyOutcome;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.EvaluationReconcileService;
import com.aievaluation.backend.service.EvaluationResultService;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class EvaluationReconcileServiceImpl implements EvaluationReconcileService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationReconcileServiceImpl.class);

    private static final Duration STALE_AFTER = Duration.ofMinutes(2);
    private static final Duration TIMEOUT = Duration.ofMinutes(45);
    private static final int BATCH_SIZE = 20;

    private final EvaluationRepository evaluationRepository;
    private final EvaluatorClient evaluatorClient;
    private final EvaluationResultService evaluationResultService;
    private final MeterRegistry meterRegistry;

    public EvaluationReconcileServiceImpl(EvaluationRepository evaluationRepository,
                                          EvaluatorClient evaluatorClient,
                                          EvaluationResultService evaluationResultService,
                                          MeterRegistry meterRegistry) {
        this.evaluationRepository = evaluationRepository;
        this.evaluatorClient = evaluatorClient;
        this.evaluationResultService = evaluationResultService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int reconcileBatch() {
        Instant now = Instant.now();
        List<UUID> ids = evaluationRepository.findStaleInFlightIds(
                EnumSet.of(EvaluationStatus.QUEUED, EvaluationStatus.RUNNING),
                now.minus(STALE_AFTER),
                PageRequest.of(0, BATCH_SIZE));
        for (UUID id : ids) {
            try {
                reconcileOne(id, now.minus(TIMEOUT));
            } catch (Exception ex) {
                // One bad row must not stop the others.
                log.error("Reconcile failed for evaluation {}: {}", id, ex.getClass().getSimpleName(), ex);
            }
        }
        return ids.size();
    }

    // No transaction here: the HTTP call happens outside one; each write is its own short transaction.
    private void reconcileOne(UUID id, Instant timeoutCutoff) {
        JobLookup lookup = evaluatorClient.getJob(id);
        meterRegistry.counter("evaluator.reconcile", "lookup", lookup.getStatus().name()).increment();

        if (lookup.getStatus() == JobLookupStatus.FOUND) {
            ApplyOutcome outcome = evaluationResultService.apply(id, lookup.getJob());
            if (outcome == ApplyOutcome.STILL_RUNNING) {
                evaluationResultService.failIfQueuedBefore(id, timeoutCutoff);
            }
        } else if (lookup.getStatus() == JobLookupStatus.NOT_FOUND) {
            evaluationResultService.markLost(id);
        } else {
            // Evaluator down or refusing us: nothing to learn this round, but still enforce the time limit.
            evaluationResultService.failIfQueuedBefore(id, timeoutCutoff);
        }
    }
}