package com.aievaluation.backend.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EvaluationStatus {
    PENDING,     // saved, not yet sent to the evaluator
    QUEUED,      // the evaluator accepted it
    RUNNING,     // seen running when we checked
    COMPLETED,   // scored
    REJECTED,    // the evaluator refused the input
    FAILED;      // evaluator error, timeout or contract problem

    public static Set<EvaluationStatus> inProgress() {
        return EnumSet.of(PENDING, QUEUED, RUNNING);
    }

    public boolean isFinal() {
        return !inProgress().contains(this);
    }
}