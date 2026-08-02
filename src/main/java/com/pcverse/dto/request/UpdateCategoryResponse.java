package com.pcverse.dto.request;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateCategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        Instant createdAt
) {
}
