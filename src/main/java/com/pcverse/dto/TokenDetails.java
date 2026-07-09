package com.pcverse.dto;

import lombok.Builder;

@Builder
public record TokenDetails(

        String value,      // Token string (JWT)
        String jwtId,      // UUID của token
        long ttlSeconds    // TTL tính bằng giây

) {
}
