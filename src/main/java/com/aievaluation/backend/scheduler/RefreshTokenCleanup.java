package com.aievaluation.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.service.RefreshTokenService;

@Component
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanup(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    // Default: every night at 03:30 IST, when almost nobody is using the app.
    @Scheduled(cron = "${app.auth.token-cleanup-cron:0 30 3 * * *}", zone = "Asia/Kolkata")
    public void run() {
        try {
            int deleted = refreshTokenService.deleteExpired();
            log.info("Refresh-token cleanup deleted {} expired token(s)", deleted);
        } catch (Exception ex) {
            log.error("Refresh-token cleanup failed: {}", ex.getClass().getSimpleName(), ex);
        }
    }
}