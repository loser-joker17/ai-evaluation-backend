package com.aievaluation.backend.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    /** The access token's subject is the user id (see JwtTokenProvider). */
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}