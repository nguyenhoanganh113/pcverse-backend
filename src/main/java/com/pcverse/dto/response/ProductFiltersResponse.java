package com.pcverse.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ProductFiltersResponse(
        String categoryId,
        String categoryName,
        List<ProductAttributeFilterResponse> filters
) {
}
