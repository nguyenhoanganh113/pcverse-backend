package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record BrandResponse(

        String id,

        String name,

        String slug,

        String logoUrl,

        boolean active,

        Long version,

        Instant createdAt,

        Instant updatedAt

) {
}
