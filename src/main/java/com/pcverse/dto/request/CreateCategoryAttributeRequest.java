package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateCategoryAttributeRequest(

        @NotBlank(message = "Attribute id must not be blank")
        String attributeDefinitionId,

        @NotNull(message = "Required must not be null")
        Boolean required,

        @NotNull(message = "Filterable must not be null")
        Boolean filterable,

        @NotNull(message = "Highlighted must not be null")
        Boolean highlighted,

        @NotNull(message = "Display order must not be null")
        @PositiveOrZero(
                message = "Display order must be greater than or equal to 0"
        )
        Integer displayOrder

) {

        public CreateCategoryAttributeRequest {
                if (attributeDefinitionId != null) {
                        attributeDefinitionId = attributeDefinitionId.strip();
                }
        }

}
