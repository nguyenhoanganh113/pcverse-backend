package com.pcverse.service.impl;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.entity.Category;
import com.pcverse.entity.CategoryAttribute;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.CategoryAttributeMapper;
import com.pcverse.repository.AttributeDefinitionRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.CategoryRepository;
import com.pcverse.repository.ProductAttributeValueRepository;
import com.pcverse.service.CategoryAttributeService;
import com.pcverse.utils.ConstraintUtils;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryAttributeMapper categoryAttributeMapper;

    @Override
    @Transactional
    public AdminCategoryAttributeResponse create(String categoryId, CreateCategoryAttributeRequest request) {

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        AttributeDefinition attributeDefinition = attributeDefinitionRepository
                .findById(request.attributeDefinitionId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND));
        if (!attributeDefinition.isActive()) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        boolean existed = categoryAttributeRepository
                .existsByCategory_IdAndAttributeDefinition_Id(categoryId, request.attributeDefinitionId());

        if (existed) {
            throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_ALREADY_EXISTS);
        }

        CategoryAttribute categoryAttribute = CategoryAttribute.builder()
                .category(category)
                .attributeDefinition(attributeDefinition)
                .required(request.required())
                .filterable(request.filterable())
                .highlighted(request.highlighted())
                .displayOrder(request.displayOrder())
                .build();

        CategoryAttribute saved;

        try {
            saved = categoryAttributeRepository.saveAndFlush(categoryAttribute);

        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(
                    exception,
                    "uk_category_attribute"
            )) {
                throw new AppException(
                        ErrorCode.CATEGORY_ATTRIBUTE_ALREADY_EXISTS
                );
            }

            throw exception;
        }

        return categoryAttributeMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCategoryAttributeResponse getById(String categoryId, String categoryAttributeId) {
        CategoryAttribute categoryAttribute =
                findCategoryAttribute(
                        categoryId,
                        categoryAttributeId
                );

        return categoryAttributeMapper.toAdminResponse(categoryAttribute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCategoryAttributeResponse> getAllByCategoryId(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryAttributeRepository
                .findAllByCategory_Id(categoryId)
                .stream()
                .map(categoryAttributeMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminCategoryAttributeResponse update(
            String categoryId,
            String categoryAttributeId,
            UpdateCategoryAttributeRequest request
    ) {
        validateUpdateRequest(request);

        CategoryAttribute categoryAttribute =
                findCategoryAttribute(
                        categoryId,
                        categoryAttributeId
                );

        validateVersion(
                categoryAttribute,
                request.version()
        );

        if (request.required() != null) {
            categoryAttribute.setRequired(
                    request.required()
            );
        }

        if (request.filterable() != null) {
            categoryAttribute.setFilterable(
                    request.filterable()
            );
        }

        if (request.highlighted() != null) {
            categoryAttribute.setHighlighted(
                    request.highlighted()
            );
        }

        if (request.displayOrder() != null) {
            categoryAttribute.setDisplayOrder(
                    request.displayOrder()
            );
        }

        try {
            categoryAttributeRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_CONCURRENT_UPDATE
            );
        }

        return categoryAttributeMapper.toAdminResponse(
                categoryAttribute
        );
    }

    @Override
    @Transactional
    public void delete(
            String categoryId,
            String categoryAttributeId,
            Long version
    ) {
        if (version == null) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_VERSION_REQUIRED
            );
        }

        if (version < 0) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_CONCURRENT_UPDATE
            );
        }

        CategoryAttribute categoryAttribute =
                findCategoryAttribute(
                        categoryId,
                        categoryAttributeId
                );

        validateVersion(
                categoryAttribute,
                version
        );

        boolean inUse = productAttributeValueRepository
                .existsByProduct_Category_IdAndAttributeDefinition_Id(
                        categoryId,
                        categoryAttribute.getAttributeDefinition().getId()
                );

        if (inUse) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_IN_USE
            );
        }

        try {
            categoryAttributeRepository.delete(categoryAttribute);
            categoryAttributeRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_CONCURRENT_UPDATE
            );
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_IN_USE
            );
        }
    }

    private void validateUpdateRequest(
            UpdateCategoryAttributeRequest request
    ) {
        if (!request.hasAnyField()) {
            throw new AppException(
                    ErrorCode.NO_FIELDS_TO_UPDATE
            );
        }
    }

    private void validateVersion(
            CategoryAttribute categoryAttribute,
            Long requestedVersion
    ) {
        if (!Objects.equals(
                categoryAttribute.getVersion(),
                requestedVersion
        )) {
            throw new AppException(
                    ErrorCode.CATEGORY_ATTRIBUTE_CONCURRENT_UPDATE
            );
        }
    }

    private CategoryAttribute findCategoryAttribute(
            String categoryId,
            String categoryAttributeId
    ) {
        return categoryAttributeRepository.findByIdAndCategory_Id(categoryAttributeId, categoryId)
                .orElseGet(() -> {
                    if (!categoryRepository.existsById(categoryId)) {
                        throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
                    }

                    throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND);
                });
    }
}
