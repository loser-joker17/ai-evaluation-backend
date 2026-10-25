package com.aievaluation.backend.service;

import java.util.UUID;

import com.aievaluation.backend.dto.response.ProgressResponse;

public interface ProgressService {

    ProgressResponse forUser(UUID userId, int last);
}