package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record CategoryDetailResponse(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        int displayOrder,
        boolean active
) {
}
