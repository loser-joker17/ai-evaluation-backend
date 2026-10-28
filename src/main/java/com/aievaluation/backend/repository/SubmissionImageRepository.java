package com.aievaluation.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aievaluation.backend.entity.SubmissionImage;

public interface SubmissionImageRepository extends JpaRepository<SubmissionImage, UUID> {

    List<SubmissionImage> findBySubmissionIdOrderByPageNo(UUID submissionId);

    Optional<SubmissionImage> findBySubmissionIdAndPageNo(UUID submissionId, Integer pageNo);
}