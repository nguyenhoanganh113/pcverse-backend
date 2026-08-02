package com.pcverse.service.impl;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UpdateCategoryResponse;
import com.pcverse.dto.response.CategoryDetailResponse;
import com.pcverse.dto.response.CreateCategoryResponse;
import com.pcverse.entity.Category;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.CategoryMapper;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.service.CategoryService;
import com.pcverse.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CATEGORY-SERVICE")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CreateCategoryResponse createCategory(CreateCategoryRequest request) {
        String name = requireValidName(request.name());
        String slug = generateRequiredSlug(name);

        if (categoryRepository.existsByNameIgnoreCase(name)
                || categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(request.description())
                .build();

        saveAndFlush(category);

        return CreateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<CategoryDetailResponse> searchCategories(CategorySearchRequest categorySearchRequest, Pageable pageable) {
        Specification<Category> specification =
                switch (categorySearchRequest.searchType()) {
                    case DEFAULT ->
                            CategorySpecification.hasKeyword(
                                    categorySearchRequest.keyword()
                            );

                    case ATTRIBUTE -> {

                        // Nếu chọn attribute seach thì phải điền đủ field lẫn value muốn search
                        validateAttributeSearch(categorySearchRequest);

                        yield CategorySpecification.hasAttribute(
                                categorySearchRequest.field(),
                                categorySearchRequest.value(),
                                categorySearchRequest.exact()
                        );
                    }
                };
        Page<Category> categoryPage = categoryRepository.findAll(
                specification,
                pageable
        );

        List<CategoryDetailResponse> categories = categoryPage
                .getContent()
                .stream()
                .map(categoryMapper::toCategoryDetailResponse)
                .toList();

        return PaginationResponse.<CategoryDetailResponse>builder()
                .currentPage(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalPages(categoryPage.getTotalPages())
                .totalElements(categoryPage.getTotalElements())
                .data(categories)
                .build();
    }

    @Override
    public CategoryDetailResponse getCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryDetailResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UpdateCategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        Category category = findCategory(id);

        if (request.isNamePresent()) {
            // Kiểm tra nếu name thuộc rq dto mà null hoặc empty throw exception
            String name = requireValidName(request.name());
            String slug = generateRequiredSlug(name);

            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)
                    || categoryRepository.existsBySlugAndIdNot(slug, id)) {
                throw new AppException(ErrorCode.CATEGORY_EXISTED);
            }

            category.setName(name);
            category.setSlug(slug);
        }


        if (request.isDescriptionPresent()) {
            category.setDescription(request.description());
        }

        saveAndFlush(category);
        return UpdateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .updatedAt(category.getLastModifiedAt())
                .build();

    }

    private String requireValidName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(ErrorCode.CATEGORY_NAME_REQUIRED);
        }

        return name.trim();
    }

    @Override
    @Transactional
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.isActive()) {
            log.info("Category {} was already soft-deleted", id);
            return;
        }

        category.setActive(false);
        saveAndFlush(category);
        log.info("Category {} soft-deleted successfully", id);
    }

    private Category findCategory(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private String generateSlug(String name) {
        return Normalizer.normalize(
                        name.trim().toLowerCase(Locale.ROOT),
                        Normalizer.Form.NFD
                )
                .replace("đ", "d")
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String generateRequiredSlug(String name) {
        String slug = generateSlug(name);
        if (slug.isBlank()) {
            throw new AppException(ErrorCode.CATEGORY_NAME_INVALID);
        }

        return slug;
    }

    private void saveAndFlush(Category category) {
        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateSlugError(exception)) {
                throw new AppException(ErrorCode.CATEGORY_EXISTED);
            }

            throw exception;
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.CATEGORY_CONCURRENT_MODIFICATION);
        }
    }

    private boolean isDuplicateSlugError(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && "uk_categories_slug".equalsIgnoreCase(constraintViolation.getConstraintName())
            ) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private void validateAttributeSearch(CategorySearchRequest request) {
        if (request.field() == null) {
            throw new AppException(
                    ErrorCode.CATEGORY_SEARCH_FIELD_REQUIRED
            );
        }

        if (!StringUtils.hasText(request.value())) {
            throw new AppException(
                    ErrorCode.CATEGORY_SEARCH_VALUE_REQUIRED
            );
        }
    }
}
