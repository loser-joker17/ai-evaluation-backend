package com.aievaluation.backend.enums;

public enum ApplyOutcome {
    APPLIED,         // status changed to the job's state
    ALREADY_FINAL,   // we already had a final result; nothing changed
    STILL_RUNNING,   // job is queued/running at the evaluator; last_checked_at updated
    NOT_FOUND        // no evaluation with this id
}