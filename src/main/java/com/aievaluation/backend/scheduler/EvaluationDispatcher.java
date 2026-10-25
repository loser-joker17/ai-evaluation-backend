package com.aievaluation.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.service.EvaluationDispatchService;

@Component
public class EvaluationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EvaluationDispatcher.class);

    private final EvaluationDispatchService dispatchService;

    public EvaluationDispatcher(EvaluationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    // fixedDelay: the next run starts 5 s after the previous one ends, so runs never overlap.
    @Scheduled(fixedDelayString = "${app.evaluator.dispatch-interval:5s}")
    public void run() {
        try {
            dispatchService.dispatchDueBatch();
        } catch (Exception ex) {
            // Never let one bad run stop the schedule.
            log.error("Dispatch run failed: {}", ex.getClass().getSimpleName(), ex);
        }
    }
}