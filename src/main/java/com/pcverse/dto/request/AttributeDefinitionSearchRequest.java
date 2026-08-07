package com.pcverse.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Locale;

public record AttributeDefinitionSearchRequest(

        @Size(
                max = 150,
                message = "Từ khóa không được vượt quá 150 ký tự"
        )
        String keyword,

        Boolean active

) {

    public AttributeDefinitionSearchRequest {
        keyword = normalizeKeyword(keyword);
    }

    private static String normalizeKeyword(String value) {
        String normalized = stripToNull(value);

        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

}
