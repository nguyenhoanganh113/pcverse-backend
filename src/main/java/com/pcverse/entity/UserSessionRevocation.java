package com.pcverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@RedisHash("user_session_revocation")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionRevocation {

    @Id
    private String sessionId;

    @TimeToLive(unit = TimeUnit.SECONDS)
    private Long expiration;
}
