package com.pcverse.service.impl;

import com.pcverse.dto.request.AdminProductSearchRequest;
import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductResponse;
import com.pcverse.entity.Brand;
import com.pcverse.entity.Category;
import com.pcverse.entity.CategoryAttribute;
import com.pcverse.entity.Product;
import com.pcverse.entity.ProductImage;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.ProductMapper;
import com.pcverse.repository.BrandRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.specification.ProductSpecification;
import com.pcverse.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = findActiveCategory(request.categoryId());
        Brand brand = findActiveBrand(request.brandId());
        String sku = normalizeSku(request.sku());
        String slug = generateRequiredSlug(request.name());

        validateSkuAvailable(sku, null);
        validateSlugAvailable(slug, null);

        Product product = Product.builder()
                .name(request.name())
                .slug(slug)
                .sku(sku)
                .description(request.description())
                .price(request.price())
                .stockQuantity(
                        request.stockQuantity() == null
                                ? 0
                                : request.stockQuantity()
                )
                .allowBackorder(Boolean.TRUE.equals(request.allowBackorder()))
                .images(copyImages(request.images()))
                .category(category)
                .brand(brand)
                .productStatus(ProductStatus.INACTIVE)
                .build();

        flushCreate(product);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<ProductResponse> searchForAdmin(
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

        Page<ProductResponse> page = productRepository
                .findAll(specification, pageable)
                .map(productMapper::toResponse);

        return PaginationResponse.<ProductResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse update(
            String id,
            UpdateProductRequest request
    ) {
        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Product product = findProduct(id);
        validateVersion(product, request.version());

        updateCategory(product, request.categoryId());
        updateBrand(product, request.brandId());
        updateSku(product, request.sku());
        updateNameAndSlug(product, request.name());

        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        if (request.allowBackorder() != null) {
            product.setAllowBackorder(request.allowBackorder());
        }
        if (request.images() != null) {
            product.setImages(copyImages(request.images()));
        }

        flushUpdate();
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(
            String id,
            UpdateProductStatusRequest request
    ) {
        Product product = findProduct(id);
        validateVersion(product, request.version());

        if (product.getProductStatus() == request.productStatus()) {
            return productMapper.toResponse(product);
        }

        if (request.productStatus() == ProductStatus.ACTIVE) {
            validateCanActivate(product);
        }

        product.setProductStatus(request.productStatus());
        flushUpdate();
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(String id, Long version) {
        if (version == null) {
            throw new AppException(ErrorCode.PRODUCT_VERSION_REQUIRED);
        }

        Product product = findProduct(id);
        validateVersion(product, version);

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

        String slug = generateRequiredSlug(requestedName);
        validateSlugAvailable(slug, product.getId());
        product.setName(requestedName);
        product.setSlug(slug);
    }

    private void validateCanActivate(Product product) {
        if (product.getImages().isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }

        // Lấy ra các CategoryAttribute required khi mà tạo product
        List<CategoryAttribute> requiredAttributes = categoryAttributeRepository
                .findAllByCategory_IdOrderByDisplayOrderAsc(
                        product.getCategory().getId()
                )
                .stream()
                .filter(CategoryAttribute::isRequired)
                .toList();

        // Thu thập ID của các thuộc tính mà sản phẩm đã có giá trị
        Set<String> valuedDefinitionIds =
                product.getAttributeValues()
                        .stream()
                        .filter(value ->
                                value.getAttributeOption() != null
                        )
                        .map(value ->
                                value.getAttributeDefinition().getId()
                        )
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
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.CATEGORY_NOT_FOUND)
                );
    }

    private Brand findActiveBrand(String id) {
        return brandRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.BRAND_NOT_FOUND)
                );
    }

    private Product findProduct(String id) {
        return productRepository.findById(id)
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

    private String generateRequiredSlug(String name) {
        String slug = Normalizer.normalize(
                        name.strip().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replace("đ", "d")
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_INVALID);
        }

        return slug;
    }

    private List<ProductImage> copyImages(List<ProductImage> images) {
        return images == null
                ? new ArrayList<>()
                : new ArrayList<>(images);
    }

    private void flushCreate(Product product) {
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uk_products_sku")) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
            }
            if (hasConstraint(exception, "uk_products_slug")) {
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
            if (hasConstraint(exception, "uk_products_sku")) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
            }
            if (hasConstraint(exception, "uk_products_slug")) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
            }
            throw new AppException(ErrorCode.PRODUCT_DATA_INTEGRITY_VIOLATION);
        }
    }

    private boolean hasConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(
                    violation.getConstraintName()
            )) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
