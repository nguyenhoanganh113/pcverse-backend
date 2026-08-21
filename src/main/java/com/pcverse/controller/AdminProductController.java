package com.pcverse.controller;

import com.pcverse.dto.request.AdminProductFilterRequest;
import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.UpdateProductConfigurationRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.AdminProductConfigurationResponse;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.AdminProductResponse;
import com.pcverse.dto.response.AdminProductAttributesResponse;
import com.pcverse.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_CREATE')")
    public ApiResponse<AdminProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.<AdminProductResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Product created successfully")
                .data(productService.create(request))
                .build();
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_VIEW')")
    public ApiResponse<AdminProductResponse> getById(@PathVariable String productId) {
        return ApiResponse.<AdminProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(productService.getById(productId))
                .build();
    }

    @GetMapping("/{productId}/attributes")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_VIEW')")
    public ApiResponse<AdminProductAttributesResponse> getAttributes(
            @PathVariable String productId
    ) {
        return ApiResponse.<AdminProductAttributesResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product attributes retrieved successfully")
                .data(productService.getAttributes(productId))
                .build();
    }

    @PutMapping("/{productId}/configuration")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_UPDATE')")
    public ApiResponse<AdminProductConfigurationResponse> updateConfiguration(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductConfigurationRequest request
    ) {
        return ApiResponse.<AdminProductConfigurationResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product configuration updated successfully")
                .data(productService.updateConfiguration(productId, request))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_VIEW')")
    public ApiResponse<PaginationResponse<AdminProductResponse>> search(
            @Valid @ModelAttribute AdminProductFilterRequest request,
            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<AdminProductResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(productService.searchForAdmin(request, pageable))
                .build();
    }

    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_UPDATE')")
    public ApiResponse<AdminProductResponse> updateStatus(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        return ApiResponse.<AdminProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product status updated successfully")
                .data(productService.updateStatus(productId, request))
                .build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String productId,
            @RequestParam
            @PositiveOrZero(message = "Version must be greater than or equal to 0")
            Long version
    ) {
        productService.delete(productId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Product deleted successfully")
                .build();
    }
}
