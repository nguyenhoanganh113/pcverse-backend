package com.pcverse.mapper;

import com.pcverse.dto.response.AddressResponse;
import com.pcverse.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(
            target = "isDefault",
            expression = "java(Boolean.TRUE.equals(address.getIsDefault()))"
    )
    AddressResponse toResponse(Address address);
}
