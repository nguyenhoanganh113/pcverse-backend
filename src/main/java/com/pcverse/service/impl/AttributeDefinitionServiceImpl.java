package com.pcverse.service.impl;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.response.AttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.AttributeDefinitionMapper;
import com.pcverse.repository.AttributeDefinitionRepository;
import com.pcverse.repository.CategoryAttributeRepository;
import com.pcverse.repository.ProductAttributeValueRepository;
import com.pcverse.repository.specification.AttributeDefinitionSpecification;
import com.pcverse.service.AttributeDefinitionService;
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
    public AttributeDefinitionResponse create(CreateAttributeDefinitionRequest request) {

        if (attributeDefinitionRepository.existsByCode(request.code())) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_ALREADY_EXISTS);
        }

        AttributeDefinition attributeDefinition = mapper.toEntity(request);
        attributeDefinition.setActive(true);

        try {
            attributeDefinitionRepository
                    .saveAndFlush(attributeDefinition);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_ALREADY_EXISTS
            );
        }

        log.info(
                "Attribute definition created successfully: attributeDefinitionId={}",
                attributeDefinition.getId()
        );

        return mapper.toResponse(attributeDefinition);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AttributeDefinitionResponse> searchForAdmin(AttributeDefinitionSearchRequest request, Pageable pageable) {

        Specification<AttributeDefinition> specification =
                Specification.allOf(
                        AttributeDefinitionSpecification.hasKeyword(
                                request.keyword()
                        ),
                        AttributeDefinitionSpecification.hasActive(
                                request.active()
                        )
                );

        Page<AttributeDefinitionResponse> page =
                attributeDefinitionRepository.findAll(specification, pageable)
                        .map(mapper::toResponse);

        return PaginationResponse
                .<AttributeDefinitionResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public AttributeDefinitionResponse getById(String id) {
        return mapper.toResponse(findAttributeDefinition(id));
    }

    @Override
    @Transactional
    public AttributeDefinitionResponse update(String id, UpdateAttributeDefinitionRequest request) {

        if (!request.hasAnyField()) {
            throw new AppException(
                    ErrorCode.NO_FIELDS_TO_UPDATE
            );
        }

        // Nếu trường name được gửi trong request nhưng giá trị của nó là null, thì ném ra exception
        if (request.isNamePresent()
                && request.getName() == null) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_NAME_REQUIRED
            );
        }

        AttributeDefinition attributeDefinition = findAttributeDefinition(id);
        validateVersion(attributeDefinition, request.getVersion());

        if (request.isNamePresent()) {
            attributeDefinition.setName(request.getName());
        }

        flushUpdate();

        return mapper.toResponse(attributeDefinition);
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
    public AttributeDefinitionResponse updateStatus(
            String id,
            UpdateAttributeDefinitionStatusRequest request
    ) {
        AttributeDefinition attributeDefinition =
                findAttributeDefinition(id);

        validateVersion(attributeDefinition, request.version());

        boolean requestedStatus = request.active();

        if (attributeDefinition.isActive() == requestedStatus) {
            log.info(
                    "Attribute definition status unchanged: attributeDefinitionId={}, active={}",
                    attributeDefinition.getId(),
                    attributeDefinition.isActive()
            );

            return mapper.toResponse(attributeDefinition);
        }

        attributeDefinition.setActive(requestedStatus);

        flushUpdate();

        log.info(
                "Attribute definition status updated successfully: attributeDefinitionId={}, active={}",
                attributeDefinition.getId(),
                attributeDefinition.isActive()
        );

        return mapper.toResponse(attributeDefinition);
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
