package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record ProductFilterOptionResponse(
        String id,
        String code,
        String label,
        int displayOrder
) {
}
