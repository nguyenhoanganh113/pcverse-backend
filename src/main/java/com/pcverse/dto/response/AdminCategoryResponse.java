package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminCategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        boolean active,
        Long version,
        String parentId,
        String parentName,
        Instant createdAt,
        Instant updatedAt
) {
}
