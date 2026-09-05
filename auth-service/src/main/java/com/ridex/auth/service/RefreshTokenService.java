package com.ridex.auth.service;

import java.util.UUID;

import com.ridex.auth.entity.RefreshToken;
import com.ridex.auth.entity.User;
import com.ridex.auth.service.util.GeneratedRefreshToken;

public interface RefreshTokenService {

    GeneratedRefreshToken create(User user);

    GeneratedRefreshToken rotate(RefreshToken currentToken, User user);

    /**
     * Revokes a single refresh token.
     */
    void revoke(String rawRefreshToken);

    /**
     * Revokes every active refresh token belonging
     * to the user.
     */
    void revokeAll(UUID userId);
    
}
