package com.pcverse.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductDetailResponse(
        String id,
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        boolean inStock,
        boolean availableForOrder,
        List<ProductImageResponse> images,
        String categoryId,
        String categoryName,
        String categorySlug,
        String brandId,
        String brandName,
        String brandSlug,
        String brandLogoUrl,
        List<ProductAttributeValueResponse> attributeValues
) {
}
