package com.pcverse.dto.request;

import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record AttributeDefinitionSearchRequest(

        @Size(
                max = 150,
                message = "Từ khóa không được vượt quá 150 ký tự"
        )
        String keyword,

        @EnumPattern(fieldName = "dataType", enumClass = AttributeDataType.class)
        String dataType,

        Boolean active

) {

    public AttributeDefinitionSearchRequest {
        keyword = normalizeKeyword(keyword);
        dataType = normalizeDataType(dataType);
    }

    public AttributeDataType toDataType() {
        return dataType == null
                ? null
                : AttributeDataType.valueOf(dataType);
    }

    private static String normalizeKeyword(String value) {
        String normalized = stripToNull(value);

        return normalized == null
                ? null
                : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeDataType(String value) {
        String normalized = stripToNull(value);

        return normalized == null
                ? null
                : normalized.toUpperCase(Locale.ROOT);
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

}
