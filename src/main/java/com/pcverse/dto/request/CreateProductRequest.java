package com.pcverse.dto.request;

import com.pcverse.entity.ProductImage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(

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
        @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0.0")
        BigDecimal price,

        @PositiveOrZero(message = "Stock quantity must be greater than or equal to 0")
        Integer stockQuantity,

        Boolean allowBackorder,

        @Valid
        List<ProductImage> images

) {
    public CreateProductRequest {
        categoryId = strip(categoryId);
        brandId = strip(brandId);
        sku = strip(sku);
        name = strip(name);
        description = description == null ? null : description.strip();
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
