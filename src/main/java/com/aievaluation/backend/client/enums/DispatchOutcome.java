package com.aievaluation.backend.client.enums;

public enum DispatchOutcome {
    ACCEPTED,      // 202 new job, or 200 job already known
    BUSY,          // 503: the evaluator's queue is full
    REJECTED,      // 400/401/403/422: our request is wrong; retrying won't help
    UNAVAILABLE    // evaluator down, unreachable, timed out or other 5xx
}