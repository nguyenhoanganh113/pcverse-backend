package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CategoryDetailResponse(
        String id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt
) {
}
