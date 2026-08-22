package com.pcverse.dto.response;

public record PublicCategoryResponse(
        String id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String parentId,
        String parentName
) {
}
