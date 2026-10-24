package com.aievaluation.backend.service;

import java.util.UUID;

import com.aievaluation.backend.dto.response.UserResponse;

public interface UserService {

    UserResponse getById(UUID userId);
}