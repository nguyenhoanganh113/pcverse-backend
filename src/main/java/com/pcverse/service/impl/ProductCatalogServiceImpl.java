package com.pcverse.service.impl;

import com.pcverse.dto.request.ProductSearchRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductDetailResponse;
import com.pcverse.dto.response.ProductAttributeFilterResponse;
import com.pcverse.dto.response.ProductFilterOptionResponse;
import com.pcverse.dto.response.ProductFiltersResponse;
import com.pcverse.dto.response.ProductSummaryResponse;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.entity.AttributeOption;
import com.pcverse.entity.Category;
import com.pcverse.entity.CategoryAttribute;
import com.pcverse.entity.Product;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.ProductMapper;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.specification.ProductSpecification;
import com.pcverse.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductSummaryResponse> search(
            ProductSearchRequest request,
            Pageable pageable
    ) {
        validatePriceRange(request);
        Map<String, Set<String>> optionIdsByDefinitionId =
                resolveAttributeFilters(request);

        Specification<Product> specification = Specification.allOf(
                ProductSpecification.isPubliclyVisible(),
                ProductSpecification.hasKeyword(request.keyword()),
                ProductSpecification.hasPrice(
                        request.minPrice(),
                        request.maxPrice()
                ),
                ProductSpecification.inStock(request.inStock()),
                ProductSpecification.hasCategory(request.categoryId()),
                ProductSpecification.hasBrand(request.brandId()),
                ProductSpecification.hasAttributeOptions(
                        optionIdsByDefinitionId
                )
        );

        Page<ProductSummaryResponse> page = productRepository
                .findAll(
                        specification,
                        buildPageable(request, pageable)
                )
                .map(productMapper::toSummaryResponse);

        return PaginationResponse.<ProductSummaryResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        String normalizedSlug = slug.strip().toLowerCase(Locale.ROOT);

        Product product = productRepository
                .findBySlugAndProductStatusAndCategory_ActiveTrueAndBrand_ActiveTrue(
                        normalizedSlug,
                        ProductStatus.ACTIVE
                )
                .orElseThrow(() -> new AppException(
                        ErrorCode.PRODUCT_NOT_FOUND
                ));

        return productMapper.toDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductFiltersResponse getFilters(String categoryId) {
        Category category = findActiveCategory(categoryId);

        List<ProductAttributeFilterResponse> filters =
                findFilterableAttributes(categoryId).stream()
                        .map(this::toFilterResponse)
                        .filter(filter -> !filter.options().isEmpty())
                        .toList();

        return ProductFiltersResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .filters(filters)
                .build();
    }

    private Map<String, Set<String>> resolveAttributeFilters(
            ProductSearchRequest request
    ) {
        if (request.attributeOptionIds().isEmpty()) {
            return Map.of();
        }

        if (request.categoryId() == null) {
            throw new AppException(
                    ErrorCode.PRODUCT_FILTER_CATEGORY_REQUIRED
            );
        }

        findActiveCategory(request.categoryId());

        Map<String, String> definitionIdByOptionId =
                findFilterableAttributes(request.categoryId()).stream()
                        .flatMap(categoryAttribute -> {
                            String definitionId = categoryAttribute
                                    .getAttributeDefinition()
                                    .getId();

                            return categoryAttribute
                                    .getAttributeDefinition()
                                    .getAttributeOptions()
                                    .stream()
                                    .filter(AttributeOption::isActive)
                                    .map(option -> Map.entry(
                                            option.getId(),
                                            definitionId
                                    ));
                        })
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ));

        boolean hasInvalidOption = request.attributeOptionIds().stream()
                .anyMatch(optionId ->
                        !definitionIdByOptionId.containsKey(optionId)
                );
        if (hasInvalidOption) {
            throw new AppException(
                    ErrorCode.PRODUCT_ATTRIBUTE_FILTER_INVALID
            );
        }

        return request.attributeOptionIds().stream()
                .collect(Collectors.groupingBy(
                        definitionIdByOptionId::get,
                        LinkedHashMap::new,
                        Collectors.toCollection(LinkedHashSet::new)
                ));
    }

    private Category findActiveCategory(String categoryId) {
        return categoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.CATEGORY_NOT_FOUND
                ));
    }

    private List<CategoryAttribute> findFilterableAttributes(
            String categoryId
    ) {
        return categoryAttributeRepository
                .findAllByCategory_IdAndFilterableTrueAndAttributeDefinition_ActiveTrueOrderByDisplayOrderAsc(
                        categoryId
                );
    }

    private ProductAttributeFilterResponse toFilterResponse(
            CategoryAttribute categoryAttribute
    ) {
        AttributeDefinition definition = categoryAttribute
                .getAttributeDefinition();

        List<ProductFilterOptionResponse> options = definition
                .getAttributeOptions()
                .stream()
                .filter(AttributeOption::isActive)
                .sorted(Comparator
                        .comparingInt(AttributeOption::getDisplayOrder)
                        .thenComparing(AttributeOption::getId))
                .map(option -> ProductFilterOptionResponse.builder()
                        .id(option.getId())
                        .code(option.getCode())
                        .label(option.getLabel())
                        .displayOrder(option.getDisplayOrder())
                        .build())
                .toList();

        return ProductAttributeFilterResponse.builder()
                .attributeDefinitionId(definition.getId())
                .attributeDefinitionCode(definition.getCode())
                .attributeDefinitionName(definition.getName())
                .displayOrder(categoryAttribute.getDisplayOrder())
                .options(options)
                .build();
    }

    private Pageable buildPageable(
            ProductSearchRequest request,
            Pageable pageable
    ) {
        Sort sort = Sort.by("createdAt").descending();

        if (request.sortBy() != null) {
            sort = switch (request.sortBy()) {
                case PRICE_ASCENDING -> Sort.by("price").ascending();
                case PRICE_DESCENDING -> Sort.by("price").descending();
                case NEWEST -> Sort.by("createdAt").descending();
                case NAME_ASCENDING -> Sort.by("name").ascending();
                case NAME_DESCENDING -> Sort.by("name").descending();
            };
        }

        sort = sort.and(Sort.by("id").ascending());

        int size = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private void validatePriceRange(ProductSearchRequest request) {
        if (request.minPrice() != null
                && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new AppException(ErrorCode.PRODUCT_PRICE_RANGE_INVALID);
        }
    }
}
