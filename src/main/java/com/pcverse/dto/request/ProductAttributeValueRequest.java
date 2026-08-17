package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProductAttributeValueRequest(

        @NotBlank(message = "Attribute definition id must not be blank")
        String attributeDefinitionId,

        @NotBlank(message = "Attribute option id must not be blank")
        String attributeOptionId

) {
    public ProductAttributeValueRequest {
        attributeDefinitionId = strip(attributeDefinitionId);
        attributeOptionId = strip(attributeOptionId);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
