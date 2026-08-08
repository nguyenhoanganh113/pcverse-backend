package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UpdateAttributeOptionRequest(
        @Pattern(
                regexp = ".*\\S.*",
                message = "Option code must not be blank"
        )
        @Size(
                max = 100,
                message = "Option code must not exceed 100 characters"
        )
        String code,

        @Pattern(
                regexp = ".*\\S.*",
                message = "Option label must not be blank"
        )
        @Size(
                max = 150,
                message = "Option label must not exceed 150 characters"
        )
        String label,

        @PositiveOrZero(
                message = "Display order must be greater than or equal to 0"
        )
        Integer displayOrder,

        @NotNull(message = "Version must not be null")
        Long version

) {
    public UpdateAttributeOptionRequest {
        if (code != null) {
            code = code.strip().toLowerCase(Locale.ROOT);
        }

        if (label != null) {
            label = label.strip();
        }
    }

    public boolean hasAnyField() {
        return code != null
                || label != null
                || displayOrder != null;
    }
}
