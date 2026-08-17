package com.pcverse.service.impl;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.response.AdminAttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.AttributeDefinitionMapper;
import com.pcverse.repository.AttributeDefinitionRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.ProductAttributeValueRepository;
import com.pcverse.repository.specification.AttributeDefinitionSpecification;
import com.pcverse.service.AttributeDefinitionService;
import com.pcverse.utils.ConstraintUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class AttributeDefinitionServiceImpl implements AttributeDefinitionService {

    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final AttributeDefinitionMapper mapper;

    @Override
    @Transactional
    public AdminAttributeDefinitionResponse create(CreateAttributeDefinitionRequest request) {

        if (attributeDefinitionRepository.existsByCode(request.code())) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_ALREADY_EXISTS);
        }

        AttributeDefinition attributeDefinition = mapper.toEntity(request);
        attributeDefinition.setActive(true);

        try {
            attributeDefinitionRepository
                    .saveAndFlush(attributeDefinition);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(
                    exception,
                    "uk_attribute_definitions_code"
            )) {
                throw new AppException(
                        ErrorCode.ATTRIBUTE_DEFINITION_ALREADY_EXISTS
                );
            }

            throw exception;
        }

        log.info(
                "Attribute definition created successfully: attributeDefinitionId={}",
                attributeDefinition.getId()
        );

        return mapper.toAdminResponse(attributeDefinition);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AdminAttributeDefinitionResponse> searchForAdmin(AttributeDefinitionSearchRequest request, Pageable pageable) {

        Specification<AttributeDefinition> specification =
                Specification.allOf(
                        AttributeDefinitionSpecification.hasKeyword(
                                request.keyword()
                        ),
                        AttributeDefinitionSpecification.hasActive(
                                request.active()
                        )
                );

        Page<AdminAttributeDefinitionResponse> page =
                attributeDefinitionRepository.findAll(specification, pageable)
                        .map(mapper::toAdminResponse);

        return PaginationResponse
                .<AdminAttributeDefinitionResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public AdminAttributeDefinitionResponse getById(String id) {
        return mapper.toAdminResponse(findAttributeDefinition(id));
    }

    @Override
    @Transactional
    public AdminAttributeDefinitionResponse update(String id, UpdateAttributeDefinitionRequest request) {

        if (!request.hasAnyField()) {
            throw new AppException(
                    ErrorCode.NO_FIELDS_TO_UPDATE
            );
        }

        AttributeDefinition attributeDefinition = findAttributeDefinition(id);
        validateVersion(attributeDefinition, request.version());

        mapper.partialUpdate(request, attributeDefinition);

        flushUpdate();

        return mapper.toAdminResponse(attributeDefinition);
    }

    @Override
    @Transactional
    public void delete(String id, Long version) {

        AttributeDefinition attributeDefinition = findAttributeDefinition(id);
        validateVersion(attributeDefinition, version);

        // Kiểm tra xem AttributeDefinition có đang được sử dụng trong CategoryAttribute hay không ?
        boolean assignedToCategory = categoryAttributeRepository.existsByAttributeDefinition_Id(id);

        // Kiểm tra xem AttributeDefinition có đang được sử dụng trong ProductAttributeValue hay không ?
        boolean usedByProduct = productAttributeValueRepository.existsByAttributeDefinition_Id(id);

        if (assignedToCategory || usedByProduct) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_IN_USE
            );
        }

        try {
            attributeDefinitionRepository.delete(attributeDefinition);
            attributeDefinitionRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_IN_USE
            );

        } catch (OptimisticLockingFailureException exception) {

            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_CONCURRENT_MODIFICATION
            );
        }

        log.info(
                "Attribute definition deleted successfully: attributeDefinitionId={}",
                id
        );

    }

    @Override
    @Transactional
    public AdminAttributeDefinitionResponse updateStatus(
            String attributeDefinitionId,
            UpdateAttributeDefinitionStatusRequest request
    ) {

        AttributeDefinition attributeDefinition = findAttributeDefinition(attributeDefinitionId);

        validateVersion(attributeDefinition, request.version());

        boolean requestedStatus = request.active();

        if (attributeDefinition.isActive() == requestedStatus) {

            log.info(
                    "Attribute definition status unchanged: attributeDefinitionId={}, active={}",
                    attributeDefinition.getId(),
                    attributeDefinition.isActive()
            );

            return mapper.toAdminResponse(attributeDefinition);
        }

        // Nếu đang inactive AttributeDefinition này thì phải kiểm tra xem trong ProductAttributeValue

        if (!requestedStatus) {

            // có product nào có status ACTIVE sử dụng AttributeDefinition này hay không ?
            boolean inUseByActiveProduct = productAttributeValueRepository
                    .existsByAttributeDefinition_IdAndProduct_ProductStatus(
                            attributeDefinitionId,
                            ProductStatus.ACTIVE
                    );

            // tương tự kiểm tra Category ACTIVE
            boolean inUseByCategoryProduct = categoryAttributeRepository
                    .existsByAttributeDefinition_IdAndCategory_ActiveTrue(
                            attributeDefinitionId
                    );

            if (inUseByActiveProduct || inUseByCategoryProduct) {
                throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_IN_USE);
            }
        }

        attributeDefinition.setActive(requestedStatus);

        flushUpdate();

        log.info(
                "Attribute definition status updated successfully: attributeDefinitionId={}, active={}",
                attributeDefinition.getId(),
                attributeDefinition.isActive()
        );

        return mapper.toAdminResponse(attributeDefinition);
    }

    private void flushUpdate() {
        try {
            attributeDefinitionRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_CONCURRENT_MODIFICATION
            );
        }
    }

    private void validateVersion(
            AttributeDefinition attributeDefinition,
            Long requestedVersion
    ) {
        if (!Objects.equals(
                attributeDefinition.getVersion(),
                requestedVersion
        )) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_CONCURRENT_MODIFICATION
            );
        }
    }

    private AttributeDefinition findAttributeDefinition(String id) {
        return attributeDefinitionRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND
                        )
                );
    }

}
