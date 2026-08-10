package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pcverse.entity.ProductImage;
import com.pcverse.enums.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDetailResponse(

        String id,

        String name,

        String description,

        BigDecimal price,

        Integer quantity,

        List<ProductImage> images,

        ProductStatus status,

        Instant createdAt

) {
}
