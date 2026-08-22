package com.pcverse.mapper;

import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.response.AdminAttributeOptionResponse;
import com.pcverse.entity.AttributeOption;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AttributeOptionMapper {

    @Mapping(
            target = "attributeDefinitionId",
            source = "attributeDefinition.id"
    )
    @Mapping(target = "attributeDefinitionName", source = "attributeDefinition.name")
    AdminAttributeOptionResponse toAdminResponse(AttributeOption attributeOption);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "code", source = "code")
    @Mapping(target = "label", source = "label")
    @Mapping(target = "displayOrder", source = "displayOrder")
    void partialUpdate(
            UpdateAttributeOptionRequest request,
            @MappingTarget AttributeOption attributeOption
    );

}
