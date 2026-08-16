package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateAttributeOptionStatusRequest(

        @NotNull(message = "Active must not be null")
        Boolean active,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version
) {
}
