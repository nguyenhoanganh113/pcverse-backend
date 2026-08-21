package com.pcverse.controller;

import com.pcverse.dto.request.ProductSearchRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductDetailResponse;
import com.pcverse.dto.response.ProductSummaryResponse;
import com.pcverse.service.ProductCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCatalogService productCatalogService;

    @GetMapping("/search")
    public ApiResponse<PaginationResponse<ProductSummaryResponse>> search(
            @Valid @ModelAttribute ProductSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<ProductSummaryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(productCatalogService.search(request, pageable))
                .build();
    }

    @GetMapping("/{slug}")
    public ApiResponse<ProductDetailResponse> getBySlug(
            @PathVariable String slug
    ) {
        return ApiResponse.<ProductDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(productCatalogService.getBySlug(slug))
                .build();
    }
}
