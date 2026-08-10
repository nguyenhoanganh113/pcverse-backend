package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateCategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        Integer displayOrder,
        Boolean active,
        Instant createdAt
) {
}
