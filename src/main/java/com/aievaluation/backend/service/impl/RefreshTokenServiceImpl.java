package com.aievaluation.backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.config.AuthProperties;
import com.aievaluation.backend.entity.RefreshToken;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.exception.UnauthorizedException;
import com.aievaluation.backend.repository.RefreshTokenRepository;
import com.aievaluation.backend.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String SESSION_EXPIRED = "Session expired. Please log in again.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, AuthProperties authProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(authProperties.getRefreshTokenTtl()));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    // noRollbackFor: when a reused token is detected we revoke all the user's tokens AND
    // return 401. Without this, the 401 exception would roll the revocation back.
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public User rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException(SESSION_EXPIRED));

        if (token.getRevokedAt() != null) {
            // This token was already used once. Someone may have copied it, so end every session.
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            throw new UnauthorizedException(SESSION_EXPIRED);
        }
        if (!token.getExpiresAt().isAfter(now)) {
            throw new UnauthorizedException(SESSION_EXPIRED);
        }

        token.setRevokedAt(now);
        return token.getUser();
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        Optional<RefreshToken> token = refreshTokenRepository.findByTokenHash(hash(rawToken));
        if (token.isPresent() && token.get().getRevokedAt() == null) {
            token.get().setRevokedAt(Instant.now());
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}