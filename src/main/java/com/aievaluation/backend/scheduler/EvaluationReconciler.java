package com.aievaluation.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.service.EvaluationReconcileService;

@Component
public class EvaluationReconciler {

    private static final Logger log = LoggerFactory.getLogger(EvaluationReconciler.class);

    private final EvaluationReconcileService reconcileService;

    public EvaluationReconciler(EvaluationReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    @Scheduled(fixedDelayString = "${app.evaluator.reconcile-interval:60s}", initialDelayString = "30s")
    public void run() {
        try {
            int checked = reconcileService.reconcileBatch();
            if (checked > 0) {
                log.debug("Reconciler checked {} evaluation(s)", checked);
            }
        } catch (Exception ex) {
            log.error("Reconcile run failed: {}", ex.getClass().getSimpleName(), ex);
        }
    }
}