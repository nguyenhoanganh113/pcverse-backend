package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @Size(max = 150, message = "Name must not exceed 150 characters")
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @PositiveOrZero(message = "Display order must be greater than or equal to 0")
        Integer displayOrder
) {
    public CreateCategoryRequest {
        name = strip(name);
        description = stripToNull(description);
        imageUrl = stripToNull(imageUrl);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
    }
}
