package com.aievaluation.backend.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EvaluationDispatchRepository {

    // Picks due PENDING rows and pushes their next_dispatch_at forward (a "lease") in one
    // statement. SKIP LOCKED: two backend instances never take the same row.
    private static final String CLAIM_SQL =
            "UPDATE evaluations SET next_dispatch_at = :leaseUntil, dispatch_count = dispatch_count + 1, "
                    + "updated_at = :now "
                    + "WHERE id IN (SELECT id FROM evaluations WHERE status = 'PENDING' AND next_dispatch_at <= :now "
                    + "             ORDER BY next_dispatch_at LIMIT :batch FOR UPDATE SKIP LOCKED) "
                    + "RETURNING id";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EvaluationDispatchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UUID> claimDue(Instant now, Instant leaseUntil, int batch) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("leaseUntil", Timestamp.from(leaseUntil))
                .addValue("batch", batch);
        return jdbcTemplate.queryForList(CLAIM_SQL, params, UUID.class);
    }
}