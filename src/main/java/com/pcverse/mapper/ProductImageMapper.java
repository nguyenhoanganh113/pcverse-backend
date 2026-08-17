package com.pcverse.mapper;

import com.pcverse.dto.response.ProductImageResponse;
import com.pcverse.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProductImageMapper {

    ProductImageResponse toResponse(ProductImage image);
}
