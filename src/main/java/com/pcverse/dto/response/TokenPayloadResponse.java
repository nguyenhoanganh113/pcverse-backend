package com.pcverse.dto.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record TokenPayloadResponse(

        Boolean isValid,
        String id,
        Set<String> authorities

) {
}
