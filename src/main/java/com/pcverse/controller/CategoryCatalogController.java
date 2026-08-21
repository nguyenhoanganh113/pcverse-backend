package com.pcverse.controller;

import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.ProductFiltersResponse;
import com.pcverse.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryCatalogController {

    private final ProductCatalogService productCatalogService;

    @GetMapping("/{categoryId}/filters")
    public ApiResponse<ProductFiltersResponse> getProductFilters(
            @PathVariable String categoryId
    ) {
        return ApiResponse.<ProductFiltersResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Product filters retrieved successfully")
                .data(productCatalogService.getFilters(categoryId))
                .build();
    }
}
