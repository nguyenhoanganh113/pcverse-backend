package com.pcverse.dto.request;

public record CategorySearchRequest(
        String keyword,
        Boolean active
) {
    public CategorySearchRequest {
        keyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.strip();
    }
}
