package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record ProductImageResponse(
        String url,
        String altText,
        int displayOrder,
        boolean primary
) {
}
