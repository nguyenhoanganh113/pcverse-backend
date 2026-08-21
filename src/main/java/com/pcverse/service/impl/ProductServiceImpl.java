package com.pcverse.service.impl;

import com.pcverse.dto.request.*;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.AdminProductAttributesResponse;
import com.pcverse.dto.response.AdminProductConfigurationResponse;
import com.pcverse.dto.response.AdminProductResponse;
import com.pcverse.entity.*;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.ProductMapper;
import com.pcverse.repository.AttributeOptionRepository;
import com.pcverse.repository.BrandRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.specification.ProductSpecification;
import com.pcverse.service.ProductService;
import com.pcverse.utils.ConstraintUtils;
import com.pcverse.utils.SlugUtils;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final AttributeOptionRepository attributeOptionRepository;
    private final ProductMapper productMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public AdminProductResponse create(CreateProductRequest request) {

        Category category = findActiveCategory(request.categoryId());

        Brand brand = findActiveBrand(request.brandId());

        String sku = normalizeSku(request.sku());

        String slug = SlugUtils.generateSlug(request.name());

        if (slug.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_INVALID);
        }

        validateSkuAvailable(sku, null);
        validateSlugAvailable(slug, null);

        Product product = productMapper.toEntity(request);
        product.setSlug(slug);
        product.setSku(sku);
        product.setCategory(category);
        product.setBrand(brand);
        product.setProductStatus(ProductStatus.INACTIVE);

        flushCreate(product);
        return productMapper.toAdminResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AdminProductResponse> searchForAdmin(AdminProductFilterRequest request, Pageable pageable) {

        validatePriceRange(request);

        Pageable resolvedPageable = buildPageable(request, pageable);

        Specification<Product> specification = Specification.allOf(
                ProductSpecification.hasKeyword(request.keyword()),
                ProductSpecification.hasPrice(request.minPrice(), request.maxPrice()),
                ProductSpecification.hasStatus(request.productStatus()),
                ProductSpecification.inStock(request.inStock()),
                ProductSpecification.hasCategory(request.categoryId()),
                ProductSpecification.hasBrand(request.brandId())
        );

        Page<AdminProductResponse> page = productRepository
                .findAll(specification, resolvedPageable)
                .map(productMapper::toAdminResponse);

        return PaginationResponse.<AdminProductResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    private Pageable buildPageable(AdminProductFilterRequest request, Pageable pageable) {
        Sort sort = Sort.by("createdAt").descending();
        if (request.sortBy() != null) {
            sort = switch (request.sortBy()) {
                case PRICE_ASCENDING -> Sort.by("price").ascending();
                case PRICE_DESCENDING -> Sort.by("price").descending();
                case NAME_ASCENDING -> Sort.by("name").ascending();
                case NAME_DESCENDING -> Sort.by("name").descending();
                case NEWEST -> Sort.by("createdAt").descending();
            };
        }

        // Tie-breaker để kết quả phân trang luôn ổn định
        sort = sort.and(Sort.by("id").ascending());

        int size = Math.min(pageable.getPageSize(), 100);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductResponse getById(String id) {
        return productMapper.toAdminResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductAttributesResponse getAttributes(String id) {
        Product product = findProductWithAttributeValues(id);

        return productMapper.toAdminAttributesResponse(product);
    }

    @Override
    @Transactional
    public AdminProductConfigurationResponse updateConfiguration(
            String id,
            UpdateProductConfigurationRequest request
    ) {
        Product product = findProductWithAttributeValues(id);
        validateVersion(product, request.version());

        Category category = findActiveCategory(request.categoryId());
        Brand brand = findActiveBrand(request.brandId());

        updateSku(product, request.sku());
        updateNameAndSlug(product, request.name());

        product.setCategory(category);
        product.setBrand(brand);
        productMapper.updateConfiguration(request, product);
        product.setImages(new ArrayList<>(request.images()));

        replaceAttributeValues(
                product,
                category,
                request.productAttributeValues()
        );

        if (product.getProductStatus() == ProductStatus.ACTIVE) {
            validateCanActivate(product);
        }

        flushConfigurationUpdate(product, request.version());
        return productMapper.toAdminConfigurationResponse(product);
    }

    private void replaceAttributeValues(
            Product product,
            Category category,
            List<ProductAttributeValueRequest> requestedValues
    ) {
        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository
                .findAllByCategory_Id(category.getId());

        Map<String, CategoryAttribute> categoryAttributeByDefinitionId =
                categoryAttributes.stream()
                        .collect(Collectors.toMap(
                                categoryAttribute -> categoryAttribute
                                        .getAttributeDefinition()
                                        .getId(),
                                Function.identity()
                        ));

        Map<String, AttributeOption> requestedOptionByDefinitionId =
                new HashMap<>();
        Set<String> requestedDefinitionIds = new HashSet<>();

        for (ProductAttributeValueRequest attributeRequest : requestedValues) {
            String definitionId = attributeRequest.attributeDefinitionId();

            if (!requestedDefinitionIds.add(definitionId)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_DUPLICATE);
            }

            CategoryAttribute categoryAttribute =
                    categoryAttributeByDefinitionId.get(definitionId);
            if (categoryAttribute == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_ALLOWED);
            }
            if (!categoryAttribute.getAttributeDefinition().isActive()) {
                throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
            }

            AttributeOption option = attributeOptionRepository
                    .findByIdAndAttributeDefinitionId(
                            attributeRequest.attributeOptionId(),
                            definitionId
                    )
                    .orElseThrow(() -> new AppException(
                            ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND
                    ));
            if (!option.isActive()) {
                throw new AppException(ErrorCode.ATTRIBUTE_OPTION_INACTIVE);
            }

            requestedOptionByDefinitionId.put(definitionId, option);
        }

        Map<String, ProductAttributeValue> existingValueByDefinitionId =
                product.getAttributeValues().stream()
                        .collect(Collectors.toMap(
                                value -> value.getAttributeDefinition().getId(),
                                Function.identity()
                        ));

        for (ProductAttributeValue existingValue : new ArrayList<>(product.getAttributeValues())) {
            String definitionId = existingValue
                    .getAttributeDefinition()
                    .getId();

            if (!requestedOptionByDefinitionId.containsKey(definitionId)) {
                product.removeProductAttributeValue(existingValue);
            }
        }

        requestedOptionByDefinitionId.forEach((definitionId, option) -> {
            ProductAttributeValue existingValue = existingValueByDefinitionId.get(definitionId);

            if (existingValue != null) {
                existingValue.setAttributeOption(option);
                return;
            }

            ProductAttributeValue newValue = ProductAttributeValue.builder()
                    .attributeDefinition(
                            categoryAttributeByDefinitionId
                                    .get(definitionId)
                                    .getAttributeDefinition()
                    )
                    .attributeOption(option)
                    .build();
            product.addAttributeValue(newValue);
        });
    }

    @Override
    @Transactional
    public AdminProductResponse updateStatus(String id, UpdateProductStatusRequest request) {
        Product product = findProductWithAttributeValues(id);
        validateVersion(product, request.version());

        if (product.getProductStatus() == request.productStatus()) {
            return productMapper.toAdminResponse(product);
        }

        if (request.productStatus() == ProductStatus.ACTIVE) {
            validateCanActivate(product);
        }

        product.setProductStatus(request.productStatus());
        flushUpdate();
        return productMapper.toAdminResponse(product);
    }

    @Override
    @Transactional
    public void delete(String id, Long version) {
        if (version == null) {
            throw new AppException(ErrorCode.PRODUCT_VERSION_REQUIRED);
        }

        Product product = findProduct(id);
        validateVersion(product, version);

        if (product.getProductStatus() == ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_ACTIVE_CANNOT_DELETE);
        }

        try {
            productRepository.delete(product);
            productRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION);
        }
    }

    private void flushConfigurationUpdate(
            Product product,
            Long expectedVersion
    ) {
        try {
            productRepository.flush();

            // Nếu chỉ collection attributeValues thay đổi thì @OptimisticLock
            // excluded không tự tăng version của Product. Khi đó tăng bằng CAS.
            if (Objects.equals(product.getVersion(), expectedVersion)) {
                int updatedRows = productRepository.incrementVersionIfMatches(
                        product.getId(),
                        expectedVersion
                );
                if (updatedRows == 0) {
                    throw new AppException(
                            ErrorCode.PRODUCT_CONCURRENT_MODIFICATION
                    );
                }

                entityManager.refresh(product);
            }
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_products_sku")) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
            }
            if (ConstraintUtils.hasConstraint(exception, "uk_products_slug")) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
            }
            if (ConstraintUtils.hasConstraint(
                    exception,
                    "uk_product_attribute"
            )) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_DUPLICATE);
            }
            throw new AppException(
                    ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION
            );
        }
    }

    private void updateSku(Product product, String requestedSku) {
        if (requestedSku == null) {
            return;
        }

        String sku = normalizeSku(requestedSku);
        if (product.getSku().equals(sku)) {
            return;
        }

        validateSkuAvailable(sku, product.getId());
        product.setSku(sku);
    }

    private void updateNameAndSlug(Product product, String requestedName) {
        if (requestedName == null || product.getName().equals(requestedName)) {
            return;
        }

        String slug = SlugUtils.generateSlug(requestedName);

        if (slug.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_INVALID);
        }

        validateSlugAvailable(slug, product.getId());
        product.setName(requestedName);
        product.setSlug(slug);
    }

    private void validateCanActivate(Product product) {
        if (!product.getCategory().isActive()) {
            throw new AppException(ErrorCode.CATEGORY_INACTIVE);
        }

        if (!product.getBrand().isActive()) {
            throw new AppException(ErrorCode.BRAND_INACTIVE);
        }

        if (product.getImages().isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        // Check xem product hiện tại đang xét có AttributeDefinition đang INACTIVE hay không ?
        boolean hasInactiveDefinition = product.getAttributeValues()
                .stream()
                .anyMatch(value ->
                        !value.getAttributeDefinition().isActive()
                );
        if (hasInactiveDefinition) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        // Check xem product hiện tại đang xét có AttributeOption nào đang INACTIVE hay không ?
        boolean hasInactiveOption = product.getAttributeValues()
                .stream()
                .anyMatch(value ->
                        !value.getAttributeOption().isActive()
                );
        if (hasInactiveOption) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_INACTIVE);
        }

        // Lấy ra các CategoryAttribute required khi mà tạo product
        List<CategoryAttribute> requiredAttributes = categoryAttributeRepository
                .findAllByCategory_Id(
                        product.getCategory().getId()
                )
                .stream()
                .filter(CategoryAttribute::isRequired)
                .toList();

        // Thu thập ID của các thuộc tính mà sản phẩm đã có giá trị
        Set<String> valuedDefinitionIds =
                product.getAttributeValues()
                        .stream()
                        .filter(value -> value.getAttributeOption() != null)
                        // Lọc các row trong table ProductAttributeValue đã được gán AttributeOption
                        .map(value -> value.getAttributeDefinition().getId())
                        // Lấy AttributeDefinitionId mà đã được gán AttributeOption
                        .collect(Collectors.toSet());

        // Kiểm tra sản phẩm có thiếu bất kỳ thuộc tính bắt buộc nào của category không
        boolean missingRequiredAttribute =
                requiredAttributes.stream()
                        .map(categoryAttribute ->
                                categoryAttribute
                                        .getAttributeDefinition()
                                        .getId()
                        )
                        .anyMatch(requiredId ->
                                !valuedDefinitionIds.contains(requiredId)
                        );

        if (missingRequiredAttribute) {
            throw new AppException(
                    ErrorCode.PRODUCT_REQUIRED_ATTRIBUTES_MISSING
            );
        }
    }

    private Category findActiveCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.CATEGORY_NOT_FOUND)
                );

        if (!category.isActive()) {
            throw new AppException(ErrorCode.CATEGORY_INACTIVE);
        }

        return category;
    }

    private Brand findActiveBrand(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (!brand.isActive()) {
            throw new AppException(ErrorCode.BRAND_INACTIVE);
        }

        return brand;
    }

    private Product findProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Product findProductWithAttributeValues(String id) {
        return productRepository.findWithAttributeValuesById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateVersion(Product product, Long requestedVersion) {
        if (!Objects.equals(product.getVersion(), requestedVersion)) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        }
    }

    private void validatePriceRange(AdminProductFilterRequest request) {
        if (request.minPrice() != null
                && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new AppException(ErrorCode.PRODUCT_PRICE_RANGE_INVALID);
        }
    }

    private void validateSkuAvailable(String sku, String excludedId) {
        boolean exists = excludedId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, excludedId);

        if (exists) {
            throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
        }
    }

    private void validateSlugAvailable(String slug, String excludedId) {
        boolean exists = excludedId == null
                ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, excludedId);

        if (exists) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    private String normalizeSku(String sku) {
        return sku.strip().toUpperCase(Locale.ROOT);
    }

    private void flushCreate(Product product) {
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_products_sku")) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
            }
            if (ConstraintUtils.hasConstraint(exception, "uk_products_slug")) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
            }
            throw new AppException(ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION);
        }
    }

    private void flushUpdate() {
        try {
            productRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_products_sku")) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
            }
            if (ConstraintUtils.hasConstraint(exception, "uk_products_slug")) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
            }
            throw new AppException(ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION);
        }
    }

}
