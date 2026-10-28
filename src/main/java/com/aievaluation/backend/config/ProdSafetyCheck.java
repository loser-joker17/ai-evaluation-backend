package com.aievaluation.backend.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Stops the prod backend from starting with settings that are only OK on a laptop.
 * A crash at startup is better than a site that quietly runs insecure.
 */
@Component
@Profile("prod")
public class ProdSafetyCheck {

    private static final int MIN_SECRET_LENGTH = 32;

    private final AuthProperties authProperties;
    private final CorsProperties corsProperties;
    private final EvaluatorProperties evaluatorProperties;

    public ProdSafetyCheck(AuthProperties authProperties, CorsProperties corsProperties,
                           EvaluatorProperties evaluatorProperties) {
        this.authProperties = authProperties;
        this.corsProperties = corsProperties;
        this.evaluatorProperties = evaluatorProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (!authProperties.isCookieSecure()) {
            fail("AUTH_COOKIE_SECURE must be true in prod");
        }
        if (authProperties.getJwtSecret() == null || authProperties.getJwtSecret().length() < MIN_SECRET_LENGTH) {
            fail("JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters in prod");
        }
        if (evaluatorProperties.getInternalKey() == null
                || evaluatorProperties.getInternalKey().length() < MIN_SECRET_LENGTH) {
            fail("EVALUATOR_INTERNAL_KEY must be at least " + MIN_SECRET_LENGTH + " characters in prod");
        }
        for (String origin : corsProperties.getAllowedOrigins()) {
            if (origin.contains("localhost") || origin.contains("127.0.0.1") || !origin.startsWith("https://")) {
                fail("CORS_ALLOWED_ORIGINS must list only https origins in prod, found: " + origin);
            }
        }
    }

    private static void fail(String message) {
        throw new IllegalStateException("Unsafe production config: " + message);
    }
}