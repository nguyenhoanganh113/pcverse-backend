package com.pcverse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateCategoryAttributesRequest(

        @NotNull(message = "Attributes must not be null")
        @Size(
                min = 1,
                max = 100,
                message = "Attributes must contain between 1 and 100 items"
        )
        List<@NotNull(message = "Category attribute must not be null")
                @Valid CreateCategoryAttributeRequest> attributes

) {
}
