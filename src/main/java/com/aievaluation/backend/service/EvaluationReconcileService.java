package com.aievaluation.backend.service;

public interface EvaluationReconcileService {

    /** Checks stale in-flight jobs with the evaluator. Returns how many were checked. */
    int reconcileBatch();
}