package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record ProductAttributeValueResponse(
        String id,
        String attributeDefinitionId,
        String attributeDefinitionCode,
        String attributeDefinitionName,
        String attributeOptionId,
        String attributeOptionCode,
        String attributeOptionLabel
) {
}
