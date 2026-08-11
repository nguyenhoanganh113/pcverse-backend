package com.pcverse.controller;

import com.pcverse.dto.request.AdminProductSearchRequest;
import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.request.UpdateProductAttributesRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductResponse;
import com.pcverse.dto.response.ProductAttributesResponse;
import com.pcverse.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_CREATE')")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Product created successfully")
                .data(productService.create(request))
                .build();
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_VIEW')")
    public ApiResponse<ProductResponse> getById(@PathVariable String productId) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(productService.getById(productId))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_VIEW')")
    public ApiResponse<PaginationResponse<ProductResponse>> search(
            @Valid @ModelAttribute AdminProductSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<ProductResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(productService.searchForAdmin(request, pageable))
                .build();
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_UPDATE')")
    public ApiResponse<ProductResponse> update(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product updated successfully")
                .data(productService.update(productId, request))
                .build();
    }

    @PatchMapping("/{productId}/status")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_UPDATE')")
    public ApiResponse<ProductResponse> updateStatus(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        return ApiResponse.<ProductResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product status updated successfully")
                .data(productService.updateStatus(productId, request))
                .build();
    }

    @PutMapping("/{productId}/attributes")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_UPDATE')")
    public ApiResponse<ProductAttributesResponse> updateAttributes(
            @PathVariable String productId,
            @Valid @RequestBody UpdateProductAttributesRequest request
    ) {
        return ApiResponse.<ProductAttributesResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product attributes updated successfully")
                .data(productService.updateAttributes(productId, request))
                .build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('ROLE_PRODUCT_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String productId,
            @RequestParam(required = false) Long version
    ) {
        productService.delete(productId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Product deleted successfully")
                .build();
    }
}
