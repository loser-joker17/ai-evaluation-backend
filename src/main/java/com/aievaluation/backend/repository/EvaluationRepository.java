package com.aievaluation.backend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aievaluation.backend.entity.Evaluation;
import com.aievaluation.backend.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findFirstBySubmissionIdOrderByAttemptNoDesc(UUID submissionId);

    boolean existsBySubmissionIdAndStatusIn(UUID submissionId, Collection<EvaluationStatus> statuses);

    // Latest attempt per submission, for a page of the history list (one query, not one per row).
    @Query("select e from Evaluation e where e.submission.id in :submissionIds "
            + "and e.attemptNo = (select max(e2.attemptNo) from Evaluation e2 where e2.submission = e.submission)")
    List<Evaluation> findLatestForSubmissions(@Param("submissionIds") Collection<UUID> submissionIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evaluation e where e.id = :id")
    Optional<Evaluation> findByIdForUpdate(@Param("id") UUID id);

    // QUEUED/RUNNING rows we haven't checked since the cutoff, oldest check first.
    // Uses idx_evaluations_inflight (last_checked_at) from V4.
    @Query("select e.id from Evaluation e "
            + "where e.status in :statuses and (e.lastCheckedAt is null or e.lastCheckedAt < :cutoff) "
            + "order by e.lastCheckedAt asc nulls first")
    List<UUID> findStaleInFlightIds(@Param("statuses") Collection<EvaluationStatus> statuses, @Param("cutoff") Instant cutoff, Pageable limit);
    @Query("select e from Evaluation e where e.submission.user.id = :userId and e.status = :status "
            + "order by e.finishedAt desc")
    List<Evaluation> findRecentByUserAndStatus(@Param("userId") UUID userId, @Param("status") EvaluationStatus status, Pageable limit);
}