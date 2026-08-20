package com.pcverse.dto.request;

import com.pcverse.enums.ProductStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminProductFilterRequest(
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
        BigDecimal maxPrice,

        SortBy sortBy
) {
    public AdminProductFilterRequest {
        keyword = stripToNull(keyword);
        categoryId = stripToNull(categoryId);
        brandId = stripToNull(brandId);
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }

    public enum SortBy {
        PRICE_ASCENDING,
        PRICE_DESCENDING,
        NEWEST,
        NAME_ASCENDING,
        NAME_DESCENDING
    }

}
