package com.pcverse.mapper;

import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.response.AttributeDefinitionResponse;
import com.pcverse.entity.AttributeDefinition;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttributeDefinitionMapper {

    AttributeDefinitionResponse toResponse(AttributeDefinition attributeDefinition);

    AttributeDefinition toEntity(CreateAttributeDefinitionRequest request);

}
