package com.pcverse.mapper;

import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.response.AdminProductResponse;
import com.pcverse.entity.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = ProductImageMapper.class
)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "productStatus", ignore = true)
    @Mapping(target = "attributeValues", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(
            target = "stockQuantity",
            source = "stockQuantity",
            defaultValue = "0"
    )
    @Mapping(
            target = "allowBackorder",
            source = "allowBackorder",
            defaultValue = "false"
    )
    @Mapping(
            target = "images",
            expression = "java(request.images() == null "
                    + "? new java.util.ArrayList<>() "
                    + ": new java.util.ArrayList<>(request.images()))"
    )
    Product toEntity(CreateProductRequest request);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    @Mapping(target = "allowBackorder", source = "allowBackorder")
    void partialUpdate(
            UpdateProductRequest request,
            @MappingTarget Product product
    );

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    AdminProductResponse toResponse(Product product);

}
