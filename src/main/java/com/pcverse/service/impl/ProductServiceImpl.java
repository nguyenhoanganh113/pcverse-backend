package com.pcverse.service.impl;

import com.pcverse.dto.request.AdminProductSearchRequest;
import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.ProductAttributeValueRequest;
import com.pcverse.dto.request.UpdateProductAttributesRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.AdminProductAttributesResponse;
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
import org.springframework.data.domain.Pageable;
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
    public PaginationResponse<AdminProductResponse> searchForAdmin(
            AdminProductSearchRequest request,
            Pageable pageable
    ) {
        validatePriceRange(request);

        Specification<Product> specification = Specification.allOf(
                ProductSpecification.hasKeyword(request.keyword()),
                ProductSpecification.hasPrice(request.minPrice(), request.maxPrice()),
                ProductSpecification.hasStatus(request.productStatus()),
                ProductSpecification.inStock(request.inStock()),
                ProductSpecification.hasCategory(request.categoryId()),
                ProductSpecification.hasBrand(request.brandId())
        );

        Page<AdminProductResponse> page = productRepository
                .findAll(specification, pageable)
                .map(productMapper::toAdminResponse);

        return PaginationResponse.<AdminProductResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
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
    public AdminProductResponse update(
            String id,
            UpdateProductRequest request
    ) {
        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Product product = findProductWithAttributeValues(id);
        validateVersion(product, request.version());

        updateCategory(product, request.categoryId());
        updateBrand(product, request.brandId());
        updateSku(product, request.sku());
        updateNameAndSlug(product, request.name());

        productMapper.partialUpdate(request, product);

        if (request.images() != null) {
            product.setImages(new ArrayList<>(request.images()));
        }

        if (product.getProductStatus() == ProductStatus.ACTIVE) {
            validateCanActivate(product);
        }

        flushUpdate();
        return productMapper.toAdminResponse(product);
    }

    @Override
    @Transactional
    public AdminProductResponse updateStatus(
            String id,
            UpdateProductStatusRequest request
    ) {
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
    public AdminProductAttributesResponse updateAttributes(String id, UpdateProductAttributesRequest request) {
        Product product = findProductWithAttributeValues(id);
        validateVersion(product, request.version());

        List<CategoryAttribute> categoryAttributes = categoryAttributeRepository
                .findAllByCategory_Id(product.getCategory().getId());

        // Key sẽ là id của AttributeDefinition, Value là CategoryAttribute
        Map<String, CategoryAttribute> categoryAttributeByDefinitionId =
                categoryAttributes.stream()
                        .collect(Collectors.toMap(
                                categoryAttribute -> categoryAttribute
                                        .getAttributeDefinition()
                                        .getId(),
                                categoryAttribute -> categoryAttribute
                        ));

        Map<String, AttributeOption> selectedOptionByDefinitionId = new HashMap<>();

        Set<String> requestDefinitionIds = new HashSet<>();
        for (ProductAttributeValueRequest attributeRequest : request.productAttributeValues()) {

            String attributeDefinitionId = attributeRequest.attributeDefinitionId();

            // Check duplicate của attributeDefinition trong request để tránh trường hợp gửi cùng 1 attributeDefinition
            if (!requestDefinitionIds.add(attributeDefinitionId)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_DUPLICATE);
            }

            // Check từng AttributeDefinition có gán với Category của product đang xét hay không
            CategoryAttribute categoryAttribute = categoryAttributeByDefinitionId.get(attributeDefinitionId);
            if (categoryAttribute == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_ALLOWED);
            }
            // Check AttributeDefinition của từng categoryAttribute đang xét xem có active hay không
            if (!categoryAttribute.getAttributeDefinition().isActive()) {
                throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
            }

            AttributeOption attributeOption = attributeOptionRepository
                    .findByIdAndAttributeDefinitionId(attributeRequest.attributeOptionId(), attributeDefinitionId)
                    .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND));
            if (!attributeOption.isActive()) {
                throw new AppException(ErrorCode.ATTRIBUTE_OPTION_INACTIVE);
            }

            selectedOptionByDefinitionId.put(attributeDefinitionId, attributeOption);
        }

        if (product.getProductStatus() == ProductStatus.ACTIVE) {
            boolean missingRequiredAttribute =  categoryAttributes.stream()
                    .filter(CategoryAttribute::isRequired)
                    .map(categoryAttribute ->
                            categoryAttribute
                                    .getAttributeDefinition()
                                    .getId()
                    )
                    .anyMatch(requiredAttributeDefinitionId ->
                            !requestDefinitionIds.contains(requiredAttributeDefinitionId));

            if (missingRequiredAttribute) {
                throw new AppException(ErrorCode.PRODUCT_REQUIRED_ATTRIBUTES_MISSING);
            }
        }

        Map<String, ProductAttributeValue> existingValueByDefinitionId = product.getAttributeValues().stream()
                        .collect(Collectors.toMap(
                                value -> value.getAttributeDefinition().getId(),
                                Function.identity()
                        ));

        // Remove
        for (ProductAttributeValue existingValue : new ArrayList<>(product.getAttributeValues())) {

            String definitionId = existingValue.getAttributeDefinition().getId();

            // Nếu trong updateDTO không còn AttributeDefinitionId nào thì bỏ liên kết trong table ProducAttribteValue
            if (!selectedOptionByDefinitionId.containsKey(definitionId)) {
                product.removeProductAttributeValue(existingValue);
            }

        }

        // add cái AttributeDefinition mới, gán quan hệ trong bảng trung gian ProductAttributeValue
        for (Map.Entry<String, AttributeOption> entry : selectedOptionByDefinitionId.entrySet()) {

            String definitionId = entry.getKey();
            AttributeOption attributeOption = entry.getValue();

            ProductAttributeValue existingValue = existingValueByDefinitionId.get(definitionId);
            if (existingValue != null) {
                existingValue.setAttributeOption(attributeOption);
                continue;
            }

            AttributeDefinition attributeDefinition = categoryAttributeByDefinitionId.get(definitionId).getAttributeDefinition();
            ProductAttributeValue newValue = ProductAttributeValue.builder()
                    .attributeDefinition(attributeDefinition)
                    .attributeOption(attributeOption)
                    .build();

            product.addAttributeValue(newValue);
        }

        flushAttributeUpdate(product, request.version());
        return productMapper.toAdminAttributesResponse(product);
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

    private void updateCategory(Product product, String requestedCategoryId) {
        if (requestedCategoryId == null
                || product.getCategory().getId().equals(requestedCategoryId)) {
            return;
        }

        if (!product.getAttributeValues().isEmpty()) {
            throw new AppException(
                    ErrorCode.PRODUCT_CATEGORY_CHANGE_NOT_ALLOWED
            );
        }

        product.setCategory(findActiveCategory(requestedCategoryId));
    }

    private void flushAttributeUpdate(Product product, Long expectedVersion) {
        try {
            productRepository.flush();

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
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_product_attribute")) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_DUPLICATE);
            }
            throw new AppException(ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION);
        }
    }

    private void updateBrand(Product product, String requestedBrandId) {
        if (requestedBrandId == null
                || product.getBrand().getId().equals(requestedBrandId)) {
            return;
        }

        product.setBrand(findActiveBrand(requestedBrandId));
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
                .orElseThrow(() ->
                        new AppException(ErrorCode.BRAND_NOT_FOUND)
                );

        if (!brand.isActive()) {
            throw new AppException(ErrorCode.BRAND_INACTIVE);
        }

        return brand;
    }

    private Product findProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.PRODUCT_NOT_FOUND)
                );
    }

    private Product findProductWithAttributeValues(String id) {
        return productRepository.findByIdWithAttributeValues(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.PRODUCT_NOT_FOUND)
                );
    }

    private void validateVersion(Product product, Long requestedVersion) {
        if (!Objects.equals(product.getVersion(), requestedVersion)) {
            throw new AppException(ErrorCode.PRODUCT_CONCURRENT_MODIFICATION);
        }
    }

    private void validatePriceRange(AdminProductSearchRequest request) {
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
