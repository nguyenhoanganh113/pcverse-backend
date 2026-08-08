package com.pcverse.service.impl;

import com.pcverse.dto.request.AttributeOptionCreateRequest;
import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.AttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.entity.AttributeOption;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.AttributeOptionMapper;
import com.pcverse.repository.AttributeDefinitionRepository;
import com.pcverse.repository.AttributeOptionRepository;
import com.pcverse.repository.ProductAttributeValueRepository;
import com.pcverse.repository.specification.AttributeOptionSpecification;
import com.pcverse.service.AttributeOptionService;
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
@Slf4j
@AllArgsConstructor
public class AttributeOptionServiceImpl implements AttributeOptionService {

    private final AttributeOptionRepository attributeOptionRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final AttributeOptionMapper attributeOptionMapper;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    @Override
    @Transactional
    public AttributeOptionResponse create(String attributeDefinitionId,
                                          AttributeOptionCreateRequest request) {

        AttributeDefinition attributeDefinition = attributeDefinitionRepository
                .findById(attributeDefinitionId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND));

        /*
         * Chỉ thuộc tính đang active mới được tạo AttributeOption.
         */
        if (!attributeDefinition.isActive()) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        if (attributeOptionRepository.existsByAttributeDefinitionIdAndCodeIgnoreCase(attributeDefinitionId, request.code())) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS);
        }

        AttributeOption attributeOption = AttributeOption.builder()
                .code(request.code())
                .label(request.label())
                .displayOrder(request.displayOrder())
                .active(true)
                .build();

        attributeDefinition.addAttributeOption(attributeOption);

        try {
            AttributeOption saved = attributeOptionRepository.saveAndFlush(attributeOption);

            return attributeOptionMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            /*
             * Phòng trường hợp hai request đồng thời cùng tạo một code.
             * Unique constraint trong database sẽ là lớp bảo vệ cuối cùng.
             */
            throw new AppException(
                    ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeOptionResponse getById(String attributeDefinitionId, String attributeOptionId) {
        AttributeOption attributeOption = attributeOptionRepository
                .findByIdAndAttributeDefinitionId(attributeOptionId, attributeDefinitionId)
                .orElseGet(() -> {
                    if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
                    }
                    throw new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND);
                });

        return attributeOptionMapper.toResponse(attributeOption);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AttributeOptionResponse> searchForAdmin(String attributeDefinitionId, AttributeOptionSearchRequest request, Pageable pageable) {

        if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND
            );
        }

        Specification<AttributeOption> specification =
                Specification.allOf(
                        AttributeOptionSpecification.belongsToAttributeDefinition(attributeDefinitionId),
                        AttributeOptionSpecification.hasKeyword(request.keyword()),
                        AttributeOptionSpecification.hasActive(request.active())
                );

        Page<AttributeOptionResponse> page =
                attributeOptionRepository.findAll(specification, pageable)
                        .map(attributeOptionMapper::toResponse);

        return PaginationResponse
                .<AttributeOptionResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional
    public void delete(String attributeDefinitionId, String attributeOptionId, Long version) {

        AttributeOption attributeOption = attributeOptionRepository
                .findByIdAndAttributeDefinitionId(attributeOptionId, attributeDefinitionId)
                .orElseGet(() -> {
                    if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
                    }
                    throw new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND);
                });

        validateVersion(attributeOption, version);

        boolean inUse = productAttributeValueRepository
                .existsByAttributeOption_Id(attributeOptionId);

        if (inUse) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_IN_USE);
        }

        try {
            attributeOptionRepository.delete(attributeOption);
            attributeOptionRepository.flush();

        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_OPTION_CONCURRENT_UPDATE
            );
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_IN_USE);
        }

        log.info(
                "Attribute option deleted successfully: attributeDefinitionId={}, attributeOptionId={}",
                attributeDefinitionId,
                attributeOptionId
        );
    }

    @Override
    @Transactional
    public AttributeOptionResponse update(String attributeDefinitionId, String attributeOptionId, UpdateAttributeOptionRequest request) {

        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        AttributeOption attributeOption = attributeOptionRepository
                .findByIdAndAttributeDefinitionId(attributeOptionId, attributeDefinitionId)
                .orElseGet(() -> {
                    if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
                    }
                    throw new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND);
                });

        validateVersion(attributeOption, request.version());

        if (request.code() != null && !request.code().equalsIgnoreCase(attributeOption.getCode())) {

            boolean codeExists = attributeOptionRepository
                    .existsByAttributeDefinitionIdAndCodeIgnoreCaseAndIdNot(
                            attributeDefinitionId,
                            request.code(),
                            attributeOptionId
                    );

            if (codeExists) {
                throw new AppException(ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS);
            }

            attributeOption.setCode(request.code());
        }

        if (request.label() != null) {
            attributeOption.setLabel(request.label());
        }

        if (request.displayOrder() != null) {
            attributeOption.setDisplayOrder(request.displayOrder());
        }

        try {
            attributeOptionRepository.flush();
            return attributeOptionMapper.toResponse(attributeOption);
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_CONCURRENT_UPDATE);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS);
        }
    }
    @Override
    @Transactional
    public AttributeOptionResponse updateStatus(
            String attributeDefinitionId,
            String attributeOptionId,
            UpdateAttributeOptionStatusRequest request) {

        AttributeOption attributeOption = attributeOptionRepository
                .findByIdAndAttributeDefinitionId(
                        attributeOptionId,
                        attributeDefinitionId
                )
                .orElseGet(() -> {
                    if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
                    }

                    throw new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND);
                });

        validateVersion(attributeOption, request.version());

        boolean newStatus = request.active();

        if (attributeOption.isActive() == newStatus) {
            return attributeOptionMapper.toResponse(attributeOption);
        }

        if (newStatus && !attributeOption.getAttributeDefinition().isActive()) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        attributeOption.setActive(newStatus);

        try {
            attributeOptionRepository.flush();
            return attributeOptionMapper.toResponse(attributeOption);
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.ATTRIBUTE_OPTION_CONCURRENT_UPDATE
            );
        }
    }

    private void validateVersion(AttributeOption attributeOption, Long requestedVersion) {
        if (!Objects.equals(attributeOption.getVersion(), requestedVersion)) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_CONCURRENT_UPDATE);
        }
    }
}
