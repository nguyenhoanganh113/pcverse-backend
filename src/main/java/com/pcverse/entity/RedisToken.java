package com.pcverse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

@RedisHash("redis_token")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RedisToken {

    @Id
    private String jwtId;

    /* @Indexed:
    Mục đích: Tạo secondary index cho field userId

    Cách hoạt động:
        Redis sẽ tạo thêm một set để index userId
        Cho phép query tokens theo userId
        Hữu ích khi cần thu hồi tất cả tokens của một user

    Use case:
        Khi user đổi password → thu hồi tất cả tokens của user đó
        Khi admin block user → vô hiệu hóa tất cả tokens
        Xem danh sách tokens đang active của một user
     */
    @Indexed
    private String userId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;


}
