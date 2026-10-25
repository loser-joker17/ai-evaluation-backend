package com.aievaluation.backend.service;

import java.util.UUID;

import com.aievaluation.backend.dto.response.QuotaResponse;
import com.aievaluation.backend.entity.User;

public interface QuotaService {

    /** Takes one of today's submission slots, or throws 429 if none are left. */
    void consume(User user);

    QuotaResponse getQuota(UUID userId);
}