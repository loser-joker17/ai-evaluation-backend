package com.aievaluation.backend.mapper;

import org.springframework.stereotype.Component;

import com.aievaluation.backend.dto.response.UserResponse;
import com.aievaluation.backend.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole().name());
        response.setEmailVerified(user.isEmailVerified());
        response.setResearchConsent(user.isResearchConsent());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}