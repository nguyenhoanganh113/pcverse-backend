package com.pcverse.service.impl;

import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.AttributeOptionCreateRequest;
import com.pcverse.dto.request.BulkCreateAttributeOptionsRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.AdminAttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.entity.AttributeDefinition;
import com.pcverse.entity.AttributeOption;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.AttributeOptionMapper;
import com.pcverse.repository.AttributeDefinitionRepository;
import com.pcverse.repository.AttributeOptionRepository;
import com.pcverse.repository.ProductAttributeValueRepository;
import com.pcverse.repository.specification.AttributeOptionSpecification;
import com.pcverse.service.AttributeOptionService;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    public List<AdminAttributeOptionResponse> createBulk(
            String attributeDefinitionId,
            BulkCreateAttributeOptionsRequest request
    ) {

        AttributeDefinition attributeDefinition = attributeDefinitionRepository
                .findById(attributeDefinitionId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND));

        /*
         * Chỉ thuộc tính đang active mới được tạo AttributeOption.
         */
        if (!attributeDefinition.isActive()) {
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }

        Set<String> requestedCodes = new HashSet<>();
        for (AttributeOptionCreateRequest item : request.options()) {
            if (!requestedCodes.add(item.code())) {
                throw new AppException(ErrorCode.ATTRIBUTE_OPTION_DUPLICATE);
            }
        }

        if (attributeOptionRepository.existsAnyByAttributeDefinitionIdAndCodes(
                attributeDefinitionId,
                requestedCodes
        )) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS);
        }

        List<AttributeOption> newOptions = new ArrayList<>();
        for (AttributeOptionCreateRequest item : request.options()) {
            AttributeOption attributeOption = AttributeOption.builder()
                    .code(item.code())
                    .label(item.label())
                    .displayOrder(item.displayOrder())
                    .active(false)
                    .build();

            attributeDefinition.addAttributeOption(attributeOption);
            newOptions.add(attributeOption);
        }

        try {
            attributeOptionRepository.saveAll(newOptions);
            attributeOptionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            /*
             * Phòng trường hợp hai request đồng thời cùng tạo một code.
             * Unique constraint trong database sẽ là lớp bảo vệ cuối cùng.
             */
            if (ConstraintUtils.hasConstraint(
                    exception,
                    "uk_attribute_option_code"
            )) {
                throw new AppException(
                        ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS
                );
            }

            throw exception;
        }

        return newOptions.stream()
                .sorted(Comparator
                        .comparingInt(AttributeOption::getDisplayOrder)
                        .thenComparing(AttributeOption::getId))
                .map(attributeOptionMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAttributeOptionResponse getById(String attributeDefinitionId, String attributeOptionId) {
        AttributeOption attributeOption = attributeOptionRepository
                .findByIdAndAttributeDefinitionId(attributeOptionId, attributeDefinitionId)
                .orElseGet(() -> {
                    if (!attributeDefinitionRepository.existsById(attributeDefinitionId)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_NOT_FOUND);
                    }
                    throw new AppException(ErrorCode.ATTRIBUTE_OPTION_NOT_FOUND);
                });

        return attributeOptionMapper.toAdminResponse(attributeOption);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<AdminAttributeOptionResponse> searchForAdmin(String attributeDefinitionId, AttributeOptionSearchRequest request, Pageable pageable) {

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

        Page<AdminAttributeOptionResponse> page =
                attributeOptionRepository.findAll(specification, pageable)
                        .map(attributeOptionMapper::toAdminResponse);

        return PaginationResponse
                .<AdminAttributeOptionResponse>builder()
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
    public AdminAttributeOptionResponse update(String attributeDefinitionId, String attributeOptionId, UpdateAttributeOptionRequest request) {

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
        }

        attributeOptionMapper.partialUpdate(request, attributeOption);

        try {
            attributeOptionRepository.flush();
            return attributeOptionMapper.toAdminResponse(attributeOption);
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.ATTRIBUTE_OPTION_CONCURRENT_UPDATE);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(
                    exception,
                    "uk_attribute_option_code"
            )) {
                throw new AppException(
                        ErrorCode.ATTRIBUTE_OPTION_ALREADY_EXISTS
                );
            }

            throw exception;
        }
    }
    @Override
    @Transactional
    public AdminAttributeOptionResponse updateStatus(
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

        boolean requestedActive = request.active();

        if (attributeOption.isActive() == requestedActive) {
            return attributeOptionMapper.toAdminResponse(attributeOption);
        }

        if (!requestedActive) {
            // Có Product ACTIVE nào đang sử dụng AttributeOption này hay không?
            boolean inUseByActiveProduct = productAttributeValueRepository
                    .existsByAttributeOption_IdAndProduct_ProductStatus(
                            attributeOptionId,
                            ProductStatus.ACTIVE
                    );

            if (inUseByActiveProduct) {
                throw new AppException(ErrorCode.ATTRIBUTE_OPTION_IN_USE);
            }
        } else if (!attributeOption.getAttributeDefinition().isActive()) {
            // Chỉ được activate AttributeOption khi AttributeDefinition vẫn active.
            throw new AppException(ErrorCode.ATTRIBUTE_DEFINITION_INACTIVE);
        }
        attributeOption.setActive(requestedActive);

        try {
            attributeOptionRepository.flush();
            return attributeOptionMapper.toAdminResponse(attributeOption);
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
