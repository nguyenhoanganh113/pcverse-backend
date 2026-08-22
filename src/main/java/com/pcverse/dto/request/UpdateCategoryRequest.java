package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(

        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        String description,

        String parentId,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version
) {
    public UpdateCategoryRequest {
        name = strip(name);
        parentId = stripToNull(parentId);
        description = stripToNull(description);
        imageUrl = stripToNull(imageUrl);
    }

    public boolean hasAnyField() {
        return name != null || description != null || imageUrl != null || parentId != null;
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
