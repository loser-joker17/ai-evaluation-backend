package com.aievaluation.backend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationStatus;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findFirstBySubmissionIdOrderByAttemptNoDesc(UUID submissionId);

    boolean existsBySubmissionIdAndStatusIn(UUID submissionId, Collection<EvaluationStatus> statuses);
}