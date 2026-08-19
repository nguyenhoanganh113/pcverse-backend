package com.pcverse.dto.request;

import jakarta.validation.constraints.Size;

public record CategoryAttributeSearchRequest(

        @Size(
                max = 150,
                message = "Keyword must not exceed 150 characters"
        )
        String keyword,

        Boolean required,

        Boolean filterable,

        Boolean highlighted
) {

    public CategoryAttributeSearchRequest {
        if (keyword != null) {
            keyword = keyword.strip();
        }
    }
}
