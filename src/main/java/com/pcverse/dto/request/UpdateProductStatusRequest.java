package com.pcverse.dto.request;

import com.pcverse.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductStatusRequest(

        @NotNull(message = "Product status must not be null")
        ProductStatus productStatus,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(message = "Version must be greater than or equal to 0")
        Long version

) {
}
