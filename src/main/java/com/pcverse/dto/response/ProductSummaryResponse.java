package com.pcverse.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductSummaryResponse(
        String id,
        String name,
        String slug,
        String sku,
        BigDecimal price,
        boolean inStock,
        boolean availableForOrder,
        ProductImageResponse primaryImage,
        String categoryId,
        String categoryName,
        String categorySlug,
        String brandId,
        String brandName,
        String brandSlug
) {
}
