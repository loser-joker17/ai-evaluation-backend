package com.aievaluation.backend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aievaluation.backend.dto.response.UserResponse;
import com.aievaluation.backend.entity.User;
import com.aievaluation.backend.exception.ResourceNotFoundException;
import com.aievaluation.backend.mapper.UserMapper;
import com.aievaluation.backend.repository.UserRepository;
import com.aievaluation.backend.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
}