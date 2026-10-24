package com.aievaluation.backend.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.config.AuthProperties;

@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    // The browser only sends the cookie to /api/auth/*, never to the rest of the API.
    private static final String PATH = "/api/auth";

    private final AuthProperties authProperties;

    public RefreshTokenCookie(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String create(String refreshToken) {
        return build(refreshToken, authProperties.getRefreshTokenTtl());
    }

    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(authProperties.isCookieSecure())
                .sameSite(authProperties.getCookieSameSite())
                .path(PATH)
                .maxAge(maxAge)
                .build()
                .toString();
    }
}