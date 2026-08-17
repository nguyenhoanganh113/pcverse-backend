package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCategoryAttributeRequest(

        Boolean required,

        Boolean filterable,

        Boolean highlighted,

        @PositiveOrZero(
                message = "Display order must be greater than or equal to 0"
        )
        Integer displayOrder,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version

) {
    public boolean hasAnyField() {
        return required != null
                || filterable != null
                || highlighted != null
                || displayOrder != null;
    }
}
