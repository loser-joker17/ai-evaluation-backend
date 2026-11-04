package com.aievaluation.backend.security;

/** What we use from a verified Google ID token. */
public class GoogleIdentity {

    private final String subject;
    private final String email;
    private final boolean emailVerified;
    private final String name;

    public GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
        this.subject = subject;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
    }

    public String getSubject() { return subject; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getName() { return name; }
}