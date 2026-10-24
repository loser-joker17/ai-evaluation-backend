package com.aievaluation.backend.mapper;

import org.springframework.stereotype.Component;

import com.aievaluation.backend.dto.response.AuthResponse;
import com.aievaluation.backend.service.AuthResult;

@Component
public class AuthMapper {

    private final UserMapper userMapper;

    public AuthMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthResponse toResponse(AuthResult result) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(result.getAccessToken());
        response.setExpiresInSeconds(result.getAccessTokenExpiresInSeconds());
        response.setUser(userMapper.toResponse(result.getUser()));
        return response;
    }
}