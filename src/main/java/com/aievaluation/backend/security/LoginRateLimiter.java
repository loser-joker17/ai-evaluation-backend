package com.aievaluation.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.exception.TooManyRequestsException;

/**
 * Fixed-window counters for failed logins and registrations, kept in memory.
 * Fine for one backend instance; move to Postgres if the backend is ever scaled out.
 */
@Component
public class LoginRateLimiter {

    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_FAILURES_PER_EMAIL = 5;
    private static final int MAX_FAILURES_PER_IP = 20;

    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);
    private static final int MAX_REGISTRATIONS_PER_IP = 10;

    private static final String LOGIN_BLOCKED =
            "Too many failed login attempts. Please try again in 15 minutes.";
    private static final String REGISTER_BLOCKED =
            "Too many accounts created from this network. Please try again later.";

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Call before checking the password. Throws 429 if this email or IP is blocked. */
    public void checkLogin(String email, String ip) {
        if (isBlocked("login-email:" + email, MAX_FAILURES_PER_EMAIL, LOGIN_WINDOW)
                || isBlocked("login-ip:" + ip, MAX_FAILURES_PER_IP, LOGIN_WINDOW)) {
            throw new TooManyRequestsException(LOGIN_BLOCKED);
        }
    }

    public void recordLoginFailure(String email, String ip) {
        increment("login-email:" + email, LOGIN_WINDOW);
        increment("login-ip:" + ip, LOGIN_WINDOW);
    }

    /** A correct password clears the email's counter (not the IP's). */
    public void recordLoginSuccess(String email) {
        windows.remove("login-email:" + email);
    }

    /** Counts every registration attempt from this IP; throws 429 over the limit. */
    public void checkAndRecordRegistration(String ip) {
        String key = "register-ip:" + ip;
        if (isBlocked(key, MAX_REGISTRATIONS_PER_IP, REGISTER_WINDOW)) {
            throw new TooManyRequestsException(REGISTER_BLOCKED);
        }
        increment(key, REGISTER_WINDOW);
    }

    // Drops finished windows so the map can't grow without limit.
    @Scheduled(fixedDelay = 600_000)
    public void removeExpired() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private boolean isBlocked(String key, int max, Duration window) {
        Window current = windows.get(key);
        return current != null && !current.isExpired(Instant.now()) && current.getCount() >= max;
    }

    private void increment(String key, Duration window) {
        Instant now = Instant.now();
        // compute() is atomic per key; Window is immutable, so readers never see half an update.
        windows.compute(key, (k, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Window(now.plus(window), 1);
            }
            return new Window(current.getEndsAt(), current.getCount() + 1);
        });
    }

    private static final class Window {

        private final Instant endsAt;
        private final int count;

        private Window(Instant endsAt, int count) {
            this.endsAt = endsAt;
            this.count = count;
        }

        Instant getEndsAt() { return endsAt; }

        int getCount() { return count; }

        boolean isExpired(Instant now) { return !now.isBefore(endsAt); }
    }
}