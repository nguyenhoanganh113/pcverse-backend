package com.pcverse.mapper;

import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.UpdateProductConfigurationRequest;
import com.pcverse.dto.response.AdminProductAttributesResponse;
import com.pcverse.dto.response.AdminProductConfigurationResponse;
import com.pcverse.dto.response.AdminProductResponse;
import com.pcverse.dto.response.ProductImageResponse;
import com.pcverse.dto.response.ProductDetailResponse;
import com.pcverse.dto.response.ProductSummaryResponse;
import com.pcverse.entity.Product;
import com.pcverse.entity.ProductImage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Comparator;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {
                ProductImageMapper.class,
                ProductAttributeValueMapper.class
        }
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

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "description", source = "description")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "stockQuantity", source = "stockQuantity")
    @Mapping(target = "allowBackorder", source = "allowBackorder")
    void updateConfiguration(
            UpdateProductConfigurationRequest request,
            @MappingTarget Product product
    );

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(target = "updatedAt", source = "lastModifiedAt")
    AdminProductResponse toAdminResponse(Product product);

    @Mapping(target = "productId", source = "id")
    @Mapping(target = "productName", source = "name")
    AdminProductAttributesResponse toAdminAttributesResponse(Product product);

    @Mapping(target = "product", source = ".")
    @Mapping(target = "attributeValues", source = "attributeValues")
    AdminProductConfigurationResponse toAdminConfigurationResponse(
            Product product
    );

    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(
            target = "availableForOrder",
            expression = "java(product.isAvailableForOrder())"
    )
    @Mapping(
            target = "primaryImage",
            expression = "java(toPrimaryImage(product))"
    )
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "brandSlug", source = "brand.slug")
    ProductSummaryResponse toSummaryResponse(Product product);

    @Mapping(target = "inStock", expression = "java(product.isInStock())")
    @Mapping(
            target = "availableForOrder",
            expression = "java(product.isAvailableForOrder())"
    )
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "brandSlug", source = "brand.slug")
    @Mapping(target = "brandLogoUrl", source = "brand.logoUrl")
    ProductDetailResponse toDetailResponse(Product product);

    default ProductImageResponse toPrimaryImage(Product product) {
        ProductImage primaryImage = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .min(Comparator.comparingInt(ProductImage::getDisplayOrder))
                .orElseGet(() -> product.getImages().stream()
                        .min(Comparator.comparingInt(
                                ProductImage::getDisplayOrder
                        ))
                        .orElse(null));

        if (primaryImage == null) {
            return null;
        }

        return ProductImageResponse.builder()
                .url(primaryImage.getUrl())
                .altText(primaryImage.getAltText())
                .displayOrder(primaryImage.getDisplayOrder())
                .primary(primaryImage.isPrimary())
                .build();
    }

}
