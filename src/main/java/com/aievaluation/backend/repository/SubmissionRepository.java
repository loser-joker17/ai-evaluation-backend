package com.aievaluation.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aievaluation.backend.entity.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Page<Submission> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Always look up by id AND owner, so one student can never read another's answer.
    Optional<Submission> findByIdAndUserId(UUID id, UUID userId);
}