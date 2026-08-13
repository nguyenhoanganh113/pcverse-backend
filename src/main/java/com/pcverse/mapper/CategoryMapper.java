package com.pcverse.mapper;

import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.response.CategoryResponse;
import com.pcverse.entity.Category;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CategoryMapper {

    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "categoryAttributes", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "imageUrl", source = "imageUrl")
    void partialUpdate(UpdateCategoryRequest request, @MappingTarget Category category);

}
