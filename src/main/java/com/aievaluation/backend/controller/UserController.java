package com.aievaluation.backend.controller;

import java.util.UUID;

import com.aievaluation.backend.dto.response.QuotaResponse;
import com.aievaluation.backend.security.CurrentUser;
import com.aievaluation.backend.service.QuotaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aievaluation.backend.dto.response.UserResponse;
import com.aievaluation.backend.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final QuotaService quotaService;

    public UserController(UserService userService,QuotaService quotaService) {
        this.userService = userService;
        this.quotaService=quotaService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // The token's subject is the user id (set in JwtTokenProvider).
        return userService.getById(UUID.fromString(jwt.getSubject()));
    }
    @GetMapping("/me/quota")
    public QuotaResponse quota(@AuthenticationPrincipal Jwt jwt) {
        return quotaService.getQuota(CurrentUser.id(jwt));
    }
}