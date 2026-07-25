package com.pcverse.service.impl;

import com.pcverse.entity.RedisToken;
import com.pcverse.entity.UserTokenRevocation;
import com.pcverse.repository.RedisTokenRepository;
import com.pcverse.repository.UserTokenRevocationRepository;
import com.pcverse.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements RedisTokenService {

    private final RedisTokenRepository redisTokenRepository;
    private final UserTokenRevocationRepository userTokenRevocationRepository;

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
    public void revokeAllUserTokens(String userId) {
        userTokenRevocationRepository.save(
                UserTokenRevocation.builder()
                        .userId(userId)
                        .revokedAtEpochSecond(
                                Instant.now().getEpochSecond()
                        )
                        .build()
        );
    }

    @Override
    public boolean isUserTokenRevoked(
            String userId,
            Instant issuedAt
    ) {
        return userTokenRevocationRepository.findById(userId)
                .map(revocation -> issuedAt == null
                        || issuedAt.getEpochSecond()
                        <= revocation.getRevokedAtEpochSecond())
                .orElse(false);
    }
}
