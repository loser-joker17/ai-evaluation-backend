package com.aievaluation.backend.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.config.AuthProperties;
import com.aievaluation.backend.entity.User;

@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public JwtTokenProvider(JwtEncoder jwtEncoder, AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.getAccessTokenTtl()))
                .subject(user.getId().toString())        // read back as the user id
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        // Without an explicit HS256 header the encoder would try RS256 and fail.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getAccessTokenTtlSeconds() {
        return authProperties.getAccessTokenTtl().toSeconds();
    }
}