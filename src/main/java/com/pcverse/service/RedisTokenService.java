package com.pcverse.service;

import com.pcverse.entity.RedisToken;

import java.time.Instant;

public interface RedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);

    void revokeAllUserTokens(String keycloakId);

    boolean isUserTokenRevoked(String keycloakId, Instant issuedAt);
}
