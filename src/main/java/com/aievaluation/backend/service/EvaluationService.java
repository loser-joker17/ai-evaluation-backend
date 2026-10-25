package com.aievaluation.backend.service;

import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;

public interface EvaluationService {

    /** Creates the next attempt for this submission, PENDING and due now. */
    Evaluation createAttempt(Submission submission);
}