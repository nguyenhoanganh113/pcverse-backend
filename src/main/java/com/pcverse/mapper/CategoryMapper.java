package com.pcverse.mapper;

import com.pcverse.dto.response.CategoryDetailResponse;
import com.pcverse.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    CategoryDetailResponse toCategoryDetailResponse(Category category);

}
