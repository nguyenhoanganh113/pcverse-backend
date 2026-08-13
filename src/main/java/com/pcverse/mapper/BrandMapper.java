package com.pcverse.mapper;

import com.pcverse.dto.request.CreateBrandRequest;
import com.pcverse.dto.request.UpdateBrandRequest;
import com.pcverse.dto.response.BrandResponse;
import com.pcverse.entity.Brand;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BrandMapper {

    @Mapping(target = "id" , ignore = true)
    @Mapping(target = "slug" , ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "version", ignore = true)
    Brand toEntity(CreateBrandRequest request);

    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    BrandResponse toResponse(Brand brand);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    void partialUpdate(UpdateBrandRequest request, @MappingTarget Brand brand);

}
