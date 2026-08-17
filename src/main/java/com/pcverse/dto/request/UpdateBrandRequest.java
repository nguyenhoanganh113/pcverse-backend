package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateBrandRequest(

        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Size(max = 2048, message = "Logo URL must not exceed 2048 characters")
        String logoUrl,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version

) {
    public UpdateBrandRequest {
        name = strip(name);
        logoUrl = stripToNull(logoUrl);
    }

    public boolean hasAnyField() {
        return name != null || logoUrl != null;
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
