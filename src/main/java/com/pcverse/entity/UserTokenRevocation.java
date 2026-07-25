package com.pcverse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("user_token_revocation")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenRevocation {

    @Id
    private String keycloakId;

    private Long revokedAtEpochSecond;
}
