package com.aievaluation.backend.client.enums;

public enum JobLookupStatus {
    FOUND,        // evaluator knows the job; see the job body
    NOT_FOUND,    // 404: the evaluator restarted or the job expired
    UNAVAILABLE   // evaluator down, wrong key, bad response: try again next round
}