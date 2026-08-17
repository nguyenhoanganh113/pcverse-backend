package com.pcverse.mapper;

import com.pcverse.dto.response.ProductAttributeValueResponse;
import com.pcverse.entity.ProductAttributeValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProductAttributeValueMapper {

    @Mapping(target = "attributeDefinitionId", source = "attributeDefinition.id")
    @Mapping(target = "attributeDefinitionCode", source = "attributeDefinition.code")
    @Mapping(target = "attributeDefinitionName", source = "attributeDefinition.name")
    @Mapping(target = "attributeOptionId", source = "attributeOption.id")
    @Mapping(target = "attributeOptionCode", source = "attributeOption.code")
    @Mapping(target = "attributeOptionLabel", source = "attributeOption.label")
    ProductAttributeValueResponse toResponse(ProductAttributeValue productAttributeValue);
}
