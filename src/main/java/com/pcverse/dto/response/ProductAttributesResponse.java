package com.pcverse.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductAttributesResponse(
        Long version,
        List<ProductAttributeValueResponse> attributeValues
) {
}
