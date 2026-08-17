package com.pcverse.dto.request;

import com.pcverse.enums.ProductStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminProductSearchRequest(
        @Size(
                max = 255,
                message = "Keyword must not exceed 255 characters"
        )
        String keyword,

        String categoryId,

        String brandId,

        ProductStatus productStatus,

        Boolean inStock,

        @PositiveOrZero(
                message = "Minimum price must not be negative"
        )
        BigDecimal minPrice,

        @PositiveOrZero(
                message = "Maximum price must not be negative"
        )
        BigDecimal maxPrice
) {
    public AdminProductSearchRequest {
        keyword = stripToNull(keyword);
        categoryId = stripToNull(categoryId);
        brandId = stripToNull(brandId);
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }

}
