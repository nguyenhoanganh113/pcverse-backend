package com.pcverse.controller;

import com.pcverse.dto.request.AdminBrandSearchRequest;
import com.pcverse.dto.request.CreateBrandRequest;
import com.pcverse.dto.request.UpdateBrandRequest;
import com.pcverse.dto.request.UpdateBrandStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.BrandResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminBrandController {

    private final BrandService brandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(value = "hasAuthority('ROLE_BRAND_CREATE')")
    public ApiResponse<BrandResponse> createBrand(
            @Valid @RequestBody CreateBrandRequest request) {

        BrandResponse dataResponse = brandService.create(request);

        return ApiResponse.<BrandResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Brand created successfully")
                        .data(dataResponse)
                        .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_BRAND_VIEW')")
    public ApiResponse<PaginationResponse<BrandResponse>> search(
            @Valid @ModelAttribute AdminBrandSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ApiResponse.<PaginationResponse<BrandResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Brands retrieved successfully")
                .data(brandService.searchForAdmin(request, pageable))
                .build();
    }

    @PatchMapping("/{brandId}")
    @PreAuthorize("hasAuthority('ROLE_BRAND_UPDATE')")
    public ApiResponse<BrandResponse> update(
            @PathVariable String brandId,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        return ApiResponse.<BrandResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Brand updated successfully")
                .data(brandService.update(brandId, request))
                .build();
    }

    @PatchMapping("/{brandId}/status")
    @PreAuthorize("hasAuthority('ROLE_BRAND_UPDATE')")
    public ApiResponse<BrandResponse> updateStatus(
            @PathVariable String brandId,
            @Valid @RequestBody UpdateBrandStatusRequest request
    ) {
        return ApiResponse.<BrandResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Brand status updated successfully")
                .data(brandService.updateStatus(brandId, request))
                .build();
    }

    @DeleteMapping("/{brandId}")
    @PreAuthorize("hasAuthority('ROLE_BRAND_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String brandId,
            @RequestParam(required = false) Long version
    ) {
        brandService.delete(brandId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Brand deleted successfully")
                .build();
    }


}
