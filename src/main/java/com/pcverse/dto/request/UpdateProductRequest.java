package com.pcverse.dto.request;

import com.pcverse.entity.ProductImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(

        @Pattern(regexp = ".*\\S.*", message = "Category id must not be blank")
        String categoryId,

        @Pattern(regexp = ".*\\S.*", message = "Brand id must not be blank")
        String brandId,

        @Pattern(regexp = ".*\\S.*", message = "SKU must not be blank")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0.0")
        BigDecimal price,

        @PositiveOrZero(message = "Stock quantity must be greater than or equal to 0")
        Integer stockQuantity,

        Boolean allowBackorder,

        @Valid
        List<ProductImage> images,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version

) {
    public UpdateProductRequest {
        categoryId = strip(categoryId);
        brandId = strip(brandId);
        sku = strip(sku);
        name = strip(name);
        description = description == null ? null : description.strip();
    }

    public boolean hasAnyField() {
        return categoryId != null
                || brandId != null
                || sku != null
                || name != null
                || description != null
                || price != null
                || stockQuantity != null
                || allowBackorder != null
                || images != null;
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
