package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateAttributeDefinitionStatusRequest(

        @NotNull(message = "Trạng thái active không được để trống")
        Boolean active,

        @NotNull(message = "Version must not be null")
        Long version

) {
}
