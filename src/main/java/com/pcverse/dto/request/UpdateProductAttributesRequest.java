package com.pcverse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record UpdateProductAttributesRequest(

        @NotNull(message = "Attribute values must not be null")
        @Valid
        List<@NotNull(message = "Attribute value must not be null") @Valid ProductAttributeValueRequest> attributeValues,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version

) {
}
