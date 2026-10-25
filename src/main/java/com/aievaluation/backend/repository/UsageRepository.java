package com.aievaluation.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsageRepository {

    // Adds 1 to today's count only while it is below the limit, in one atomic statement.
    // Two requests at the same moment cannot both take the last slot.
    private static final String CONSUME_SQL =
            "INSERT INTO usage_daily (user_id, usage_date, submissions) VALUES (:userId, :usageDate, 1) "
                    + "ON CONFLICT (user_id, usage_date) DO UPDATE SET submissions = usage_daily.submissions + 1 "
                    + "WHERE usage_daily.submissions < :limit";

    private static final String USED_SQL =
            "SELECT submissions FROM usage_daily WHERE user_id = :userId AND usage_date = :usageDate";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UsageRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Returns true if a slot was taken, false if the daily limit is already reached. */
    public boolean tryConsume(UUID userId, LocalDate usageDate, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("usageDate", usageDate)
                .addValue("limit", limit);
        return jdbcTemplate.update(CONSUME_SQL, params) == 1;
    }

    public int getUsed(UUID userId, LocalDate usageDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("usageDate", usageDate);
        List<Integer> rows = jdbcTemplate.queryForList(USED_SQL, params, Integer.class);
        return rows.isEmpty() ? 0 : rows.get(0);
    }
}