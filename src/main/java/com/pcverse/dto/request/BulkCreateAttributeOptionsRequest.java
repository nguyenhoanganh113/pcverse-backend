package com.pcverse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateAttributeOptionsRequest(

        @NotNull(message = "Options must not be null")
        @Size(
                min = 1,
                max = 100,
                message = "Options must contain between 1 and 100 items"
        )
        List<@NotNull(message = "Attribute option must not be null")
                @Valid AttributeOptionCreateRequest> options

) {
}
