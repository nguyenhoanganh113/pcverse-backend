package com.pcverse.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminProductAttributesResponse(
        String productId,
        String productName,
        Long version,
        List<ProductAttributeValueResponse> attributeValues
) {
}
