package com.pcverse.dto.response;

import com.pcverse.entity.ProductImage;
import com.pcverse.enums.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record CreateProductResponse(

        String id,

        String name,

        String description,

        BigDecimal price,

        List<ProductImage> images,

        ProductStatus productStatus,

        Instant createdAt

) {
}
