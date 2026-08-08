package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record AttributeOptionResponse(

        String id,
        String attributeDefinitionId,
        String code,
        String label,
        int displayOrder,
        boolean active,
        Long version

) {
}
