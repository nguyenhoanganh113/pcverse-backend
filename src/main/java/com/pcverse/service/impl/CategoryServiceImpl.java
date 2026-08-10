package com.pcverse.service.impl;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryStatusRequest;
import com.pcverse.dto.response.CategoryResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.entity.Category;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.CategoryMapper;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.specification.CategorySpecification;
import com.pcverse.service.CategoryService;
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
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.name();
        String slug = generateRequiredSlug(name);

        validateCategoryAvailable(name, slug, null);

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .displayOrder(
                        request.displayOrder() == null
                                ? 0
                                : request.displayOrder()
                )
                .active(true)
                .build();

        flushCreate(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<CategoryResponse> searchForAdmin(
            CategorySearchRequest request,
            Pageable pageable
    ) {
        Specification<Category> specification = Specification.allOf(
                CategorySpecification.hasKeyword(request.keyword()),
                CategorySpecification.hasActive(request.active())
        );

        Page<CategoryResponse> page = categoryRepository
                .findAll(specification, pageable)
                .map(categoryMapper::toResponse);

        return PaginationResponse.<CategoryResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse update(
            String id,
            UpdateCategoryRequest request
    ) {
        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Category category = findCategory(id);
        validateVersion(category, request.version());

        if (request.isNamePresent()) {
            String name = requireValidName(request.name());
            String slug = generateRequiredSlug(name);
            validateCategoryAvailable(name, slug, id);
            category.setName(name);
            category.setSlug(slug);
        }

        if (request.isDescriptionPresent()) {
            category.setDescription(normalizeNullableText(request.description()));
        }

        if (request.isImageUrlPresent()) {
            category.setImageUrl(normalizeNullableText(request.imageUrl()));
        }

        if (request.isDisplayOrderPresent()) {
            category.setDisplayOrder(
                    requireUpdateDisplayOrder(request.displayOrder())
            );
        }

        flushUpdate();
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateStatus(
            String id,
            UpdateCategoryStatusRequest request
    ) {
        Category category = findCategory(id);
        validateVersion(category, request.version());

        boolean requestedActive = request.active();
        if (category.isActive() == requestedActive) {
            return categoryMapper.toResponse(category);
        }

        if (!requestedActive
                && productRepository.existsByCategory_IdAndProductStatus(
                        id,
                        ProductStatus.ACTIVE
                )) {
            throw new AppException(
                    ErrorCode.CATEGORY_HAS_ACTIVE_PRODUCTS
            );
        }

        category.setActive(requestedActive);
        flushUpdate();
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(String id, Long version) {
        if (version == null) {
            throw new AppException(ErrorCode.CATEGORY_VERSION_REQUIRED);
        }

        Category category = findCategory(id);
        validateVersion(category, version);

        boolean inUse = productRepository.existsByCategory_Id(id)
                || categoryAttributeRepository.existsByCategory_Id(id);

        if (inUse) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }

        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.CATEGORY_CONCURRENT_MODIFICATION
            );
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }
    }

    private Category findCategory(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.CATEGORY_NOT_FOUND)
                );
    }

    private void validateVersion(Category category, Long requestedVersion) {
        if (!Objects.equals(category.getVersion(), requestedVersion)) {
            throw new AppException(
                    ErrorCode.CATEGORY_CONCURRENT_MODIFICATION
            );
        }
    }

    private void validateCategoryAvailable(
            String name,
            String slug,
            String excludedId
    ) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                    || categoryRepository.existsBySlug(slug)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(
                        name,
                        excludedId
                ) || categoryRepository.existsBySlugAndIdNot(
                        slug,
                        excludedId
                );

        if (exists) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.CATEGORY_NAME_REQUIRED);
        }
        return name.strip();
    }

    private int requireUpdateDisplayOrder(Integer displayOrder) {
        if (displayOrder == null || displayOrder < 0) {
            throw new AppException(
                    ErrorCode.CATEGORY_DISPLAY_ORDER_INVALID
            );
        }
        return displayOrder;
    }

    private String normalizeNullableText(String value) {
        return value == null || value.isBlank()
                ? null
                : value.strip();
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
            throw new AppException(ErrorCode.CATEGORY_NAME_INVALID);
        }
        return slug;
    }

    private void flushCreate(Category category) {
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uk_categories_slug")) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            throw new AppException(
                    ErrorCode.CATEGORY_DATA_INTEGRITY_VIOLATION
            );
        }
    }

    private void flushUpdate() {
        try {
            categoryRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.CATEGORY_CONCURRENT_MODIFICATION
            );
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, "uk_categories_slug")) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            throw new AppException(
                    ErrorCode.CATEGORY_DATA_INTEGRITY_VIOLATION
            );
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
