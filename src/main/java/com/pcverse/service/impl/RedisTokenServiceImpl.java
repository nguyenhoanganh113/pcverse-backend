package com.pcverse.service.impl;

import com.pcverse.entity.RedisToken;
import com.pcverse.entity.UserSessionRevocation;
import com.pcverse.entity.UserTokenRevocation;
import com.pcverse.repository.RedisTokenRepository;
import com.pcverse.repository.UserSessionRevocationRepository;
import com.pcverse.repository.UserTokenRevocationRepository;
import com.pcverse.service.RedisTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RedisTokenServiceImpl implements RedisTokenService {

    private final RedisTokenRepository redisTokenRepository;
    private final UserTokenRevocationRepository userTokenRevocationRepository;
    private final UserSessionRevocationRepository userSessionRevocationRepository;
    private final long sessionRevocationTtlSeconds;

    public RedisTokenServiceImpl(
            RedisTokenRepository redisTokenRepository,
            UserTokenRevocationRepository userTokenRevocationRepository,
            UserSessionRevocationRepository userSessionRevocationRepository,
            @Value("${security.token-revocation.session-ttl-seconds:3600}")
            long sessionRevocationTtlSeconds
    ) {
        this.redisTokenRepository = redisTokenRepository;
        this.userTokenRevocationRepository = userTokenRevocationRepository;
        this.userSessionRevocationRepository = userSessionRevocationRepository;
        this.sessionRevocationTtlSeconds = sessionRevocationTtlSeconds;
    }

    @Override
    public void saveToken(RedisToken token) {
        // Lưu token vào Redis
        // Redis sẽ tự động set TTL dựa vào field expiration
        redisTokenRepository.save(token);
    }

    @Override
    public void deleteTokenByJwtId(String jwtId) {
        // Tìm token theo jwtId và xóa nếu tồn tại
        // Dùng ifPresent() để tránh exception nếu token không tồn tại
        redisTokenRepository.findById(jwtId)
                .ifPresent(redisTokenRepository::delete);
    }

    @Override
    public boolean existsByJwtId(String jwtId) {
        // Kiểm tra token có trong blacklist không
        // Return true nếu token đã bị thu hồi (đã logout)
        return redisTokenRepository.existsById(jwtId);
    }

    @Override
    public void revokeAllUserTokens(String keycloakId) {
        userTokenRevocationRepository.save(
                UserTokenRevocation.builder()
                        .keycloakId(keycloakId)
                        .revokedAtEpochSecond(Instant.now().getEpochSecond())
                        .build()
        );
    }

    @Override
    public boolean isUserTokenRevoked(String keycloakId, Instant issuedAt) {
        return userTokenRevocationRepository.findById(keycloakId)
                .map(revocation -> issuedAt == null
                        || issuedAt.getEpochSecond() <= revocation.getRevokedAtEpochSecond())
                .orElse(false);
    }

    @Override
    public void revokeUserSession(String sessionId) {
        userSessionRevocationRepository.save(
                UserSessionRevocation.builder()
                        .sessionId(sessionId)
                        .expiration(sessionRevocationTtlSeconds)
                        .build()
        );
    }

    @Override
    public boolean isUserSessionRevoked(String sessionId) {
        return userSessionRevocationRepository.existsById(sessionId);
    }
}
