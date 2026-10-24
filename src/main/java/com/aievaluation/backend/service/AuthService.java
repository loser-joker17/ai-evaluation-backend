package com.aievaluation.backend.service;

import com.aievaluation.backend.dto.request.LoginRequest;
import com.aievaluation.backend.dto.request.RegisterRequest;

public interface AuthService {

    AuthResult register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    AuthResult refresh(String refreshToken);

    void logout(String refreshToken);
}