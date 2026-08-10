package com.pcverse.mapper;

import com.pcverse.dto.response.CategoryResponse;
import com.pcverse.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CategoryMapper {

    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    CategoryResponse toResponse(Category category);
}
