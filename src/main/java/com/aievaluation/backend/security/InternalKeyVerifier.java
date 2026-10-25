package com.aievaluation.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.aievaluation.backend.config.EvaluatorProperties;
import com.aievaluation.backend.exception.UnauthorizedException;

/**
 * Checks the X-Internal-Key header on calls from the evaluator.
 * Both values are hashed first, so the comparison takes the same time whatever
 * the key's length, and MessageDigest.isEqual compares in constant time.
 */
@Component
public class InternalKeyVerifier {

    private final byte[] expectedHash;

    public InternalKeyVerifier(EvaluatorProperties evaluatorProperties) {
        // EvaluatorClientConfig already fails startup if the key is missing.
        this.expectedHash = sha256(evaluatorProperties.getInternalKey());
    }

    public void verify(String providedKey) {
        if (providedKey == null || providedKey.isEmpty()
                || !MessageDigest.isEqual(expectedHash, sha256(providedKey))) {
            throw new UnauthorizedException("Invalid or missing internal key");
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}