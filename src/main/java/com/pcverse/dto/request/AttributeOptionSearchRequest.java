package com.pcverse.dto.request;

import jakarta.validation.constraints.Size;

public record AttributeOptionSearchRequest(

        @Size(
                max = 150,
                message = "Keyword must not exceed 150 characters"
        )
        String keyword,

        Boolean active
) {
    public AttributeOptionSearchRequest {
        if (keyword != null) {
            keyword = keyword.strip();
        }
    }
}
