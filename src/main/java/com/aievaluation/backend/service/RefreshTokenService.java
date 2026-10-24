package com.aievaluation.backend.service;

import com.aievaluation.backend.entity.User;

public interface RefreshTokenService {

    /** Creates a new refresh token for the user and returns the raw value (for the cookie). */
    String issue(User user);

    /** Checks the token, revokes it, and returns its owner. The caller then issues a new one. */
    User rotate(String rawToken);

    /** Revokes the token if it exists. Unknown tokens are ignored. */
    void revoke(String rawToken);
}