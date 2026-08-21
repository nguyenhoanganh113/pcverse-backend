package com.pcverse.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminProductConfigurationResponse(
        AdminProductResponse product,
        List<ProductAttributeValueResponse> attributeValues
) {
}
