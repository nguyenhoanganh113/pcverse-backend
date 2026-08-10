package com.pcverse.mapper;

import com.pcverse.dto.response.ProductResponse;
import com.pcverse.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    ProductResponse toResponse(Product product);

}
