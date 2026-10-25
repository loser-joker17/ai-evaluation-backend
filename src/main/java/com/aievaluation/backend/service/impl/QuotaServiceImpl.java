package com.aievaluation.backend.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.config.QuotaProperties;
import com.aievaluation.backend.dto.response.QuotaResponse;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.enums.Role;
import com.aievaluation.backend.exception.TooManyRequestsException;
import com.aievaluation.backend.repository.UsageRepository;
import com.aievaluation.backend.service.QuotaService;

@Service
public class QuotaServiceImpl implements QuotaService {

    private final UsageRepository usageRepository;
    private final QuotaProperties quotaProperties;

    public QuotaServiceImpl(UsageRepository usageRepository, QuotaProperties quotaProperties) {
        this.usageRepository = usageRepository;
        this.quotaProperties = quotaProperties;
    }

    @Override
    @Transactional
    public void consume(User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        int limit = quotaProperties.getDailySubmissions();
        if (!usageRepository.tryConsume(user.getId(), today(), limit)) {
            throw new TooManyRequestsException(
                    "You have used all " + limit + " evaluations for today. The limit resets at midnight.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaResponse getQuota(UUID userId) {
        LocalDate today = today();
        int limit = quotaProperties.getDailySubmissions();
        int used = usageRepository.getUsed(userId, today);

        QuotaResponse response = new QuotaResponse();
        response.setDailyLimit(limit);
        response.setUsedToday(used);
        response.setRemainingToday(Math.max(0, limit - used));
        response.setResetsAt(today.plusDays(1).atStartOfDay(zone()).toInstant());
        return response;
    }

    private LocalDate today() {
        return LocalDate.now(zone());
    }

    private ZoneId zone() {
        return ZoneId.of(quotaProperties.getZone());
    }
}