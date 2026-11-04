package com.aievaluation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    @Size(max = 4096, message = "Google credential is too long")
    private String credential;

    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
}