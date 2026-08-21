package com.pcverse.dto.request;

import com.pcverse.entity.ProductImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductConfigurationRequest(

        @NotBlank(message = "Category id is required")
        String categoryId,

        @NotBlank(message = "Brand id is required")
        String brandId,

        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(
                value = "0.0",
                message = "Price must be greater than or equal to 0.0"
        )
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(
                message = "Stock quantity must be greater than or equal to 0"
        )
        Integer stockQuantity,

        @NotNull(message = "Allow backorder must not be null")
        Boolean allowBackorder,

        @NotNull(message = "Images must not be null")
        @Valid
        List<@NotNull(message = "Product image must not be null")
                @Valid ProductImage> images,

        @NotNull(message = "Attribute values must not be null")
        @Valid
        List<@NotNull(message = "Attribute value must not be null")
                @Valid ProductAttributeValueRequest> productAttributeValues,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(
                message = "Version must be greater than or equal to 0"
        )
        Long version

) {
    public UpdateProductConfigurationRequest {
        categoryId = strip(categoryId);
        brandId = strip(brandId);
        sku = strip(sku);
        name = strip(name);
        description = stripToNull(description);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
