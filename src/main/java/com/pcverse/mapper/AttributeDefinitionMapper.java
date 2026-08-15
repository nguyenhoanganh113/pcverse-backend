package com.pcverse.mapper;

import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.response.AttributeDefinitionResponse;
import com.pcverse.entity.AttributeDefinition;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttributeDefinitionMapper {

    AttributeDefinitionResponse toResponse(AttributeDefinition attributeDefinition);

    AttributeDefinition toEntity(CreateAttributeDefinitionRequest request);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "name", source = "name")
    void partialUpdate(
            UpdateAttributeDefinitionRequest request,
            @MappingTarget AttributeDefinition attributeDefinition
    );

}
