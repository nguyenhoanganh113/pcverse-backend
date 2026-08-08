package com.pcverse.mapper;

import com.pcverse.dto.response.AttributeOptionResponse;
import com.pcverse.entity.AttributeOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttributeOptionMapper {

    @Mapping(
            target = "attributeDefinitionId",
            source = "attributeDefinition.id"
    )
    AttributeOptionResponse toResponse(AttributeOption attributeOption);

}
