package com.pcverse.dto.request;

public record AttributeOptionSearchRequest(
        String keyword,
        Boolean active
) {
    public AttributeOptionSearchRequest {
        if (keyword != null) {
            keyword = keyword.strip();
        }
    }
}
