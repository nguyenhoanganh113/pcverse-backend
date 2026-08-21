package com.pcverse.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchRequest(

        @Size(
                max = 255,
                message = "Keyword must not exceed 255 characters"
        )
        String keyword,

        String categoryId,

        String brandId,

        @Size(
                max = 50,
                message = "At most 50 attribute options may be selected"
        )
        List<@NotBlank(message = "Attribute option id must not be blank")
                String> attributeOptionIds,

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
    public ProductSearchRequest {
        keyword = stripToNull(keyword);
        categoryId = stripToNull(categoryId);
        brandId = stripToNull(brandId);
        attributeOptionIds = attributeOptionIds == null
                ? List.of()
                : attributeOptionIds.stream()
                .map(ProductSearchRequest::stripToNull)
                .distinct()
                .toList();
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
