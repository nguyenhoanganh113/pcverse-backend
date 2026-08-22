package com.pcverse.dto.response;

import lombok.Builder;

@Builder
public record AdminAttributeOptionResponse(

        String id,
        String attributeDefinitionId,
        String attributeDefinitionName,
        String code,
        String label,
        int displayOrder,
        boolean active,
        Long version

) {
}
