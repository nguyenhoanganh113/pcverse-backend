package com.pcverse.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.pcverse.exception.UnknownJsonFieldException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @Size(max = 120, message = "Name must not exceed 120 characters")
        @NotBlank(message = "Name is required")
        String name,
        String description
) {

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new UnknownJsonFieldException(fieldName);
    }
}
