package com.aievaluation.backend.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.aievaluation.backend.config.AuthProperties;
import com.aievaluation.backend.exception.ApiException;
import com.aievaluation.backend.exception.UnauthorizedException;

/**
 * Checks a Google ID token: Google's signature (keys fetched from Google and cached),
 * expiry, issuer, and that it was issued for our client id.
 * Not a Spring JwtDecoder bean on purpose: that would clash with the decoder for our own tokens.
 */
@Component
public class GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(AuthProperties authProperties) {
        this.clientId = authProperties.getGoogleClientId() == null ? "" : authProperties.getGoogleClientId().trim();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && ISSUERS.contains(iss.toString())),
                new JwtClaimValidator<Object>(JwtClaimNames.AUD,
                        aud -> aud instanceof Collection<?> list ? list.contains(clientId) : clientId.equals(aud))));
    }

    public GoogleIdentity verify(String idToken) {
        if (clientId.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not set up yet.");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Google sign-in failed. Please try again.");
        }
        Object verified = jwt.getClaim("email_verified");
        return new GoogleIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified)),
                jwt.getClaimAsString("name"));
    }
}