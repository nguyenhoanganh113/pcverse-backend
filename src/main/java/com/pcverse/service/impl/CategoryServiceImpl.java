package com.pcverse.service.impl;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryStatusRequest;
import com.pcverse.dto.response.AdminCategoryResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.PublicCategoryResponse;
import com.pcverse.entity.Category;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.CategoryMapper;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.specification.CategorySpecification;
import com.pcverse.service.CategoryService;
import com.pcverse.utils.ConstraintUtils;
import com.pcverse.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public AdminCategoryResponse create(CreateCategoryRequest request) {

        String slug = SlugUtils.generateSlug(request.name());

       if (slug.isBlank()) {
           throw new AppException(ErrorCode.CATEGORY_NAME_INVALID);
       }

       if (categoryRepository.existsByNameIgnoreCase(request.name()) || categoryRepository.existsBySlug(slug)) {
           throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
       }

       Category category = categoryMapper.toCategory(request);
       category.setSlug(slug);
       category.setActive(false);
       if (request.parentId() != null) {
           Category parent = categoryRepository.findById(request.parentId())
                   .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

           category.setParent(parent);
       }

       try {
           categoryRepository.saveAndFlush(category);
       } catch (DataIntegrityViolationException exception) {
           if (ConstraintUtils.hasConstraint(exception, "uk_categories_slug")) {
               throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
           }
           throw exception;
       }

       return categoryMapper.toAdminResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AdminCategoryResponse> searchForAdmin(
            CategorySearchRequest request,
            Pageable pageable
    ) {
        Specification<Category> specification = CategorySpecification.hasKeyword(request.keyword());

        Page<AdminCategoryResponse> page = categoryRepository
                .findAll(specification, pageable)
                .map(categoryMapper::toAdminResponse);

        return PaginationResponse.<AdminCategoryResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<PublicCategoryResponse> searchForPublic(CategorySearchRequest request, Pageable pageable) {
        Specification<Category> specification = Specification.allOf(
                CategorySpecification.hasKeyword(request.keyword()),
                CategorySpecification.hasActive(true)
        );

        Page<PublicCategoryResponse> page = categoryRepository
                .findAll(specification, pageable)
                .map(categoryMapper::toPublicResponse);

        return PaginationResponse.<PublicCategoryResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCategoryResponse getById(String id) {
        return categoryMapper.toAdminResponse(findCategory(id));
    }

    @Override
    @Transactional
    public AdminCategoryResponse update(String id, UpdateCategoryRequest request) {
        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Category category = findCategory(id);
        validateVersion(category, request.version());

        if (request.name() != null) {
            String slug = SlugUtils.generateSlug(request.name());
            if (slug.isBlank()) {
                throw new AppException(ErrorCode.CATEGORY_NAME_INVALID);
            }

            validateCategoryAvailable(request.name(), slug, id);
            category.setSlug(slug);
        }

        categoryMapper.partialUpdate(request, category);

        if (request.parentId() != null) {
            String parentId = request.parentId();

            if (parentId.equals(category.getId())) {
                throw new AppException(ErrorCode.CATEGORY_PARENT_INVALID);
            }

            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() ->
                            new AppException(ErrorCode.CATEGORY_NOT_FOUND)
                    );

            category.setParent(parent);
        }

        flushUpdate();
        return categoryMapper.toAdminResponse(category);
    }

    @Override
    @Transactional
    public AdminCategoryResponse updateStatus(String id, UpdateCategoryStatusRequest request) {
        Category category = findCategory(id);
        validateVersion(category, request.version());

        boolean requestedActive = request.active();
        if (category.isActive() == requestedActive) {
            return categoryMapper.toAdminResponse(category);
        }

        // Nếu trạng thái đang từ inactive sang active thì
        if (requestedActive) {
            // Kiểm tra xem CategoryAttribute có row nào mà AttributeDefinition đang INACTIVE hay không ?
            validateCanActivate(id);
        }

        category.setActive(requestedActive);
        flushUpdate();
        return categoryMapper.toAdminResponse(category);
    }

    private void validateCanActivate(String categoryId) {
        boolean hasInactiveDefinition = categoryAttributeRepository
                .findAllByCategory_Id(categoryId)
                .stream()
                .anyMatch(categoryAttribute ->
                        !categoryAttribute
                                .getAttributeDefinition()
                                .isActive()
                );

        if (hasInactiveDefinition) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE
            );
        }
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
            throw new AppException(ErrorCode.CATEGORY_CONCURRENT_MODIFICATION);
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
            throw new AppException(ErrorCode.CATEGORY_CONCURRENT_MODIFICATION);
        }
    }

    private void validateCategoryAvailable(
            String name,
            String slug,
            String excludedId
    ) {
        boolean exists = excludedId == null
                ? categoryRepository.existsByNameIgnoreCase(name) || categoryRepository.existsBySlug(slug)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId) || categoryRepository.existsBySlugAndIdNot(slug, excludedId);

        if (exists) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private void flushUpdate() {
        try {
            categoryRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.CATEGORY_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_categories_slug")) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            throw new AppException(ErrorCode.CATEGORY_DATA_INTEGRITY_VIOLATION);
        }
    }
}
