package com.pcverse.mapper;

import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.entity.CategoryAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CategoryAttributeMapper {

    @Mapping(
            target = "categoryId",
            source = "category.id"
    )
    @Mapping(
            target = "categoryName",
            source = "category.name"
    )
    @Mapping(
            target = "attributeDefinitionId",
            source = "attributeDefinition.id"
    )
    @Mapping(
            target = "attributeDefinitionCode",
            source = "attributeDefinition.code"
    )
    @Mapping(
            target = "attributeDefinitionName",
            source = "attributeDefinition.name"
    )
    @Mapping(
            target = "attributeDefinitionActive",
            source = "attributeDefinition.active"
    )
    AdminCategoryAttributeResponse toAdminResponse(CategoryAttribute categoryAttribute);

}
