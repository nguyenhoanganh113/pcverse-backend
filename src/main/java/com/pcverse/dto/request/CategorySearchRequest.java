package com.pcverse.dto.request;

import com.pcverse.enums.CategorySearchField;
import com.pcverse.enums.SearchType;
import jakarta.validation.constraints.Size;

public record CategorySearchRequest(
        SearchType searchType,

        @Size(max = 255)
        String keyword,

        CategorySearchField field,

        @Size(max = 255)
        String value,

        Boolean exact
) {
    public CategorySearchRequest {
        searchType = searchType == null
                ? SearchType.DEFAULT
                : searchType;

        // Nếu không truyền gì thì null so sánh với true sẽ là false còn nếu truyền true hay false thì TRUE sẽ so sánh
        exact = Boolean.TRUE.equals(exact);
    }
}
