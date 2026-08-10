package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record CategoryAttributeResponse(
        String id,
        String categoryId,
        String categoryName,
        String attributeDefinitionId,
        String attributeDefinitionCode,
        String attributeDefinitionName,
        boolean required,
        boolean filterable,
        boolean highlighted,
        int displayOrder,
        Long version
) {
}
