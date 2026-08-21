package com.pcverse.service.impl;

import com.pcverse.dto.request.BulkCreateCategoryAttributesRequest;
import com.pcverse.dto.request.CategoryAttributeSearchRequest;
import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.dto.response.PaginationResponse;
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
import com.pcverse.repository.specification.CategoryAttributeSpecification;
import com.pcverse.service.CategoryAttributeService;
import com.pcverse.utils.ConstraintUtils;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CategoryAttributeServiceImpl implements CategoryAttributeService {

    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final CategoryAttributeMapper categoryAttributeMapper;

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
    public PaginationResponse<AdminCategoryAttributeResponse> searchForAdmin(
            String categoryId,
            CategoryAttributeSearchRequest request,
            Pageable pageable
    ) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Specification<CategoryAttribute> specification = Specification.allOf(
                CategoryAttributeSpecification.belongsToCategory(categoryId),
                CategoryAttributeSpecification.hasKeyword(request.keyword()),
                CategoryAttributeSpecification.hasRequired(request.required()),
                CategoryAttributeSpecification.hasFilterable(request.filterable()),
                CategoryAttributeSpecification.hasHighlighted(request.highlighted())
        );

        Page<AdminCategoryAttributeResponse> page = categoryAttributeRepository
                .findAll(specification, pageable)
                .map(categoryAttributeMapper::toAdminResponse);

        return PaginationResponse
                .<AdminCategoryAttributeResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
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
    public List<AdminCategoryAttributeResponse> createBulk(
            String categoryId,
            BulkCreateCategoryAttributesRequest request
    ) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Map<String, CreateCategoryAttributeRequest> requestedByDefinitionId =
                indexRequestedAttributes(request.attributes());
        Map<String, AttributeDefinition> definitionsById =
                loadAndValidateDefinitions(requestedByDefinitionId.keySet());

        boolean alreadyExists = categoryAttributeRepository
                .existsByCategory_IdAndAttributeDefinition_IdIn(
                        categoryId,
                        requestedByDefinitionId.keySet()
                );
        if (alreadyExists) {
            throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_ALREADY_EXISTS);
        }

        List<CategoryAttribute> newAttributes = new ArrayList<>();
        requestedByDefinitionId.forEach((definitionId, item) -> {
            newAttributes.add(CategoryAttribute.builder()
                    .category(category)
                    .attributeDefinition(definitionsById.get(definitionId))
                    .required(item.required())
                    .filterable(item.filterable())
                    .highlighted(item.highlighted())
                    .displayOrder(item.displayOrder())
                    .build());
        });

        try {
            categoryAttributeRepository.saveAll(newAttributes);
            categoryAttributeRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_category_attribute")) {
                throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_ALREADY_EXISTS);
            }

            throw exception;
        }

        return newAttributes.stream()
                .sorted(Comparator
                        .comparingInt(CategoryAttribute::getDisplayOrder)
                        .thenComparing(CategoryAttribute::getId))
                .map(categoryAttributeMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(
            String categoryId,
            String categoryAttributeId,
            Long version
    ) {
        if (version == null) {
            throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_VERSION_REQUIRED);
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

    private Map<String, CreateCategoryAttributeRequest> indexRequestedAttributes(
            List<CreateCategoryAttributeRequest> requestedAttributes
    ) {
        Map<String, CreateCategoryAttributeRequest> indexed = new HashMap<>();

        for (CreateCategoryAttributeRequest item : requestedAttributes) {
            if (indexed.putIfAbsent(item.attributeDefinitionId(), item) != null) {
                throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_DUPLICATE);
            }
        }

        return indexed;
    }

    private Map<String, AttributeDefinition> loadAndValidateDefinitions(
            Set<String> requestedDefinitionIds
    ) {
        if (requestedDefinitionIds.isEmpty()) {
            return Map.of();
        }

        Map<String, AttributeDefinition> definitionsById =
                attributeDefinitionRepository.findAllById(requestedDefinitionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                AttributeDefinition::getId,
                                Function.identity()
                        ));

        if (definitionsById.size() != requestedDefinitionIds.size()) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
        }

        boolean containsInactiveDefinition = definitionsById.values()
                .stream()
                .anyMatch(definition -> !definition.isActive());
        if (containsInactiveDefinition) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        return definitionsById;
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
