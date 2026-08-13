package com.pcverse.service.impl;

import com.pcverse.dto.request.AdminBrandSearchRequest;
import com.pcverse.dto.request.CreateBrandRequest;
import com.pcverse.dto.request.UpdateBrandRequest;
import com.pcverse.dto.request.UpdateBrandStatusRequest;
import com.pcverse.dto.response.BrandResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.entity.Brand;
import com.pcverse.enums.ProductStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.BrandMapper;
import com.pcverse.repository.BrandRepository;
import com.pcverse.repository.ProductRepository;
import com.pcverse.repository.specification.BrandSpecification;
import com.pcverse.service.BrandService;
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
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public BrandResponse create(CreateBrandRequest request) {

        String slug = SlugUtils.generateSlug(request.name());

        if (slug.isBlank()) {
            throw new AppException(ErrorCode.BRAND_NAME_INVALID);
        }

        if (brandRepository.existsByNameIgnoreCase(request.name())
                || brandRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        Brand brand = brandMapper.toEntity(request);
        brand.setSlug(slug);
        brand.setActive(false);

        try {
            brandRepository.saveAndFlush(brand);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_brands_slug")) {
                throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
            }
            throw exception;
        }
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(String id) {
        return brandMapper.toResponse(findBrand(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<BrandResponse> searchForAdmin(
            AdminBrandSearchRequest request,
            Pageable pageable
    ) {
        Specification<Brand> specification = Specification.allOf(
                BrandSpecification.hasKeyword(request.keyword()),
                BrandSpecification.hasActive(request.active())
        );

        Page<BrandResponse> page = brandRepository
                .findAll(specification, pageable)
                .map(brandMapper::toResponse);

        return PaginationResponse.<BrandResponse>builder()
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .data(page.getContent())
                .build();
    }

    @Override
    @Transactional
    public BrandResponse update(String id, UpdateBrandRequest request) {
        if (!request.hasAnyField()) {
            throw new AppException(ErrorCode.NO_FIELDS_TO_UPDATE);
        }

        Brand brand = findBrand(id);
        validateVersion(brand, request.version());

        updateNameAndSlug(brand, request.name());
        brandMapper.partialUpdate(request, brand);

        try {
            brandRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(ErrorCode.BRAND_CONCURRENT_MODIFICATION);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintUtils.hasConstraint(exception, "uk_brands_slug")) {
                throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
            }
            throw exception;
        }

        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse updateStatus(String id, UpdateBrandStatusRequest request) {
        Brand brand = findBrand(id);
        validateVersion(brand, request.version());

        boolean requestedActive = request.active();
        if (brand.isActive() == requestedActive) {
            return brandMapper.toResponse(brand);
        }

        if (!requestedActive
                && productRepository.existsByBrand_IdAndProductStatus(
                        id,
                        ProductStatus.ACTIVE
                )) {
            throw new AppException(ErrorCode.BRAND_HAS_ACTIVE_PRODUCTS);
        }

        brand.setActive(requestedActive);

        try {
            brandRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.BRAND_CONCURRENT_MODIFICATION
            );
        }

        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    public void delete(String id, Long version) {
        if (version == null) {
            throw new AppException(ErrorCode.BRAND_VERSION_REQUIRED);
        }

        Brand brand = findBrand(id);
        validateVersion(brand, version);

        if (productRepository.existsByBrand_Id(id)) {
            throw new AppException(ErrorCode.BRAND_IN_USE);
        }

        try {
            brandRepository.delete(brand);
            brandRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw new AppException(
                    ErrorCode.BRAND_CONCURRENT_MODIFICATION
            );
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.BRAND_IN_USE);
        }
    }

    private void updateNameAndSlug(Brand brand, String requestedName) {
        if (requestedName == null || brand.getName().equals(requestedName)) {
            return;
        }

        String slug = SlugUtils.generateSlug(requestedName);
        if (slug.isBlank()) {
            throw new AppException(ErrorCode.BRAND_NAME_INVALID);
        }

        boolean brandExists = brandRepository
                .existsByNameIgnoreCaseAndIdNot(
                        requestedName,
                        brand.getId()
                ) || brandRepository.existsBySlugAndIdNot(
                        slug,
                        brand.getId()
                );
        if (brandExists) {
            throw new AppException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        brand.setSlug(slug);
    }

    private Brand findBrand(String id) {
        return brandRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.BRAND_NOT_FOUND)
                );
    }

    private void validateVersion(Brand brand, Long requestedVersion) {
        if (!Objects.equals(brand.getVersion(), requestedVersion)) {
            throw new AppException(
                    ErrorCode.BRAND_CONCURRENT_MODIFICATION
            );
        }
    }
}
