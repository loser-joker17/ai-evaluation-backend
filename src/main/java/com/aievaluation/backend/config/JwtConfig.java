package com.aievaluation.backend.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

    private static final int MIN_SECRET_BYTES = 32;   // HS256 needs at least 256 bits

    private final AuthProperties authProperties;

    public JwtConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    public SecretKey jwtSecretKey() {
        String secret = authProperties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is missing. Set it in .env.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secret.trim());
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Checks expiry and that the token was issued by this backend.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(authProperties.getIssuer()));
        return decoder;
    }
}