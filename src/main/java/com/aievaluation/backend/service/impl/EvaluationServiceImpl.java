package com.aievaluation.backend.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.entity.Submission;
import com.aievaluation.backend.enums.EvaluationStatus;
import com.aievaluation.backend.repository.EvaluationRepository;
import com.aievaluation.backend.service.EvaluationService;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;

    public EvaluationServiceImpl(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    @Transactional
    public Evaluation createAttempt(Submission submission) {
        int attemptNo = 1;
        Optional<Evaluation> previous =
                evaluationRepository.findFirstBySubmissionIdOrderByAttemptNoDesc(submission.getId());
        if (previous.isPresent()) {
            attemptNo = previous.get().getAttemptNo() + 1;
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setSubmission(submission);
        evaluation.setAttemptNo(attemptNo);
        evaluation.setStatus(EvaluationStatus.PENDING);
        evaluation.setMaxMarks(submission.getMaxMarks());
        evaluation.setNextDispatchAt(Instant.now());
        return evaluationRepository.save(evaluation);
    }
}