package com.aievaluation.backend.dto.response;

public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserResponse user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }
}