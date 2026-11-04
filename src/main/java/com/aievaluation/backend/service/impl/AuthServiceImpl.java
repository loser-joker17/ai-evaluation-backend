package com.aievaluation.backend.service.impl;

import java.time.Instant;
import java.util.Optional;

import com.aievaluation.backend.dto.request.GoogleLoginRequest;
import com.aievaluation.backend.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aievaluation.backend.security.GoogleIdentity;
import com.aievaluation.backend.security.GoogleTokenVerifier;

import com.aievaluation.backend.dto.request.LoginRequest;
import com.aievaluation.backend.dto.request.RegisterRequest;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.exception.ConflictException;
import com.aievaluation.backend.exception.ForbiddenException;
import com.aievaluation.backend.exception.UnauthorizedException;
import com.aievaluation.backend.repository.UserRepository;
import com.aievaluation.backend.security.JwtTokenProvider;
import com.aievaluation.backend.service.AuthResult;
import com.aievaluation.backend.service.AuthService;
import com.aievaluation.backend.service.RefreshTokenService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final GoogleTokenVerifier googleTokenVerifier;

    // Checked against when the email is unknown, so a wrong email takes as long as a wrong
    // password and response time doesn't reveal which emails have accounts.
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService,
                           LoginRateLimiter loginRateLimiter, GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
        this.loginRateLimiter = loginRateLimiter;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request, String clientIp) {
        loginRateLimiter.checkAndRecordRegistration(clientIp);
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return buildResult(user);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());
        // Before BCrypt: a blocked caller gets 429 without costing a password check.
        loginRateLimiter.checkLogin(email, clientIp);

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || found.get().getPasswordHash() == null) {
            // Unknown email, or a Google-only account: same work, same message.
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash);
            loginRateLimiter.recordLoginFailure(email, clientIp);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginRateLimiter.recordLoginFailure(email, clientIp);
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        loginRateLimiter.recordLoginSuccess(email);
        if (!user.isEnabled()) {
            throw new ForbiddenException("This account is disabled");
        }

        user.setLastLoginAt(Instant.now());
        return buildResult(user);
    }

    // Same reason as in RefreshTokenServiceImpl.rotate: keep the "revoke all" on token reuse.
    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Session expired. Please log in again.");
        }
        User user = refreshTokenService.rotate(refreshToken);
        if (!user.isEnabled()) {
            throw new ForbiddenException("This account is disabled");
        }
        return buildResult(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    @Override
    @Transactional
    public AuthResult loginWithGoogle(GoogleLoginRequest request, String clientIp) {
        GoogleIdentity google = googleTokenVerifier.verify(request.getCredential());
        if (!google.isEmailVerified() || google.getEmail() == null) {
            throw new UnauthorizedException("Your Google account's email is not verified.");
        }
        String email = normalizeEmail(google.getEmail());

        // Returning Google user, else an existing email+password account with the same
        // verified email (linked, so the student keeps their history), else a new account.
        User user = userRepository.findByGoogleSub(google.getSubject())
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));
        if (user == null) {
            loginRateLimiter.checkAndRecordRegistration(clientIp);
            user = new User();
            user.setEmail(email);
            user.setName(displayName(google.getName(), email));
        } else if (user.getGoogleSub() != null && !user.getGoogleSub().equals(google.getSubject())) {
            throw new ConflictException("This email is linked to a different Google account.");
        }
        if (!user.isEnabled()) {
            throw new ForbiddenException("This account is disabled");
        }

        user.setGoogleSub(google.getSubject());
        user.setEmailVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return buildResult(user);
    }

    private String displayName(String name, String email) {
        String value = (name == null || name.isBlank()) ? email.substring(0, email.indexOf('@')) : name.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }
    private AuthResult buildResult(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = refreshTokenService.issue(user);
        return new AuthResult(accessToken, jwtTokenProvider.getAccessTokenTtlSeconds(), newRefreshToken, user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}