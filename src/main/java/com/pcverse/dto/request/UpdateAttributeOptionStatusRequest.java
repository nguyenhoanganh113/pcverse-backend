package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAttributeOptionStatusRequest(
        @NotNull(message = "Active must not be null")
        Boolean active,
        @NotNull(message = "Version must not be null")
        Long version
) {
}
