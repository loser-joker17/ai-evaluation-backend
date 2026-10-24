package com.aievaluation.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private String role;
    private boolean emailVerified;
    private boolean researchConsent;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isResearchConsent() { return researchConsent; }
    public void setResearchConsent(boolean researchConsent) { this.researchConsent = researchConsent; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}