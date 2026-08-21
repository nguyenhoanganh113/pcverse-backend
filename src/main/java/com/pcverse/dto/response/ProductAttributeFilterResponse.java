package com.pcverse.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductAttributeFilterResponse(
        String attributeDefinitionId,
        String attributeDefinitionCode,
        String attributeDefinitionName,
        int displayOrder,
        List<ProductFilterOptionResponse> options
) {
}
