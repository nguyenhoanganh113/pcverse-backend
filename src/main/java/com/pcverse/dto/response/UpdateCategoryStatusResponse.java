package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UpdateCategoryStatusResponse(

        String id,
        String name,
        Boolean active,
        Instant updatedAt

) {
}
