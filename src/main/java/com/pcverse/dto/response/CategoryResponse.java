package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
