package com.pcverse.service.impl;

import com.pcverse.entity.RedisToken;
import com.pcverse.repository.RedisTokenRepository;
import com.pcverse.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements RedisTokenService {

    private final RedisTokenRepository redisTokenRepository;

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
}
