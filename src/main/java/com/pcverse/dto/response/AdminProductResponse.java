package com.pcverse.dto.response;

import com.pcverse.enums.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record AdminProductResponse(
        String id,
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        int stockQuantity,
        boolean allowBackorder,
        boolean inStock,
        List<ProductImageResponse> images,
        String categoryId,
        String categoryName,
        String brandId,
        String brandName,
        ProductStatus productStatus,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
