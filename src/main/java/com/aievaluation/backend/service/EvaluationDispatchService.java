package com.aievaluation.backend.service;

public interface EvaluationDispatchService {

    /** Sends the due PENDING evaluations to the evaluator. Returns how many were tried. */
    int dispatchDueBatch();
}