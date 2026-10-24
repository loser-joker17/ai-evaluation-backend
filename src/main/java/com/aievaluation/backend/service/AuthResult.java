package com.aievaluation.backend.service;

import com.aievaluation.backend.entity.User;

public class AuthResult {

    private final String accessToken;
    private final long accessTokenExpiresInSeconds;
    private final String refreshToken;
    private final User user;

    public AuthResult(String accessToken, long accessTokenExpiresInSeconds, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public long getAccessTokenExpiresInSeconds() { return accessTokenExpiresInSeconds; }
    public String getRefreshToken() { return refreshToken; }
    public User getUser() { return user; }
}