package com.pcverse.dto.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record TokenPayloadResponse(

        Boolean isValid,
        String userId,
        String jwtId,
        Set<String> authorities

) {
}
