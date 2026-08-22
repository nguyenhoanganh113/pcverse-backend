package com.pcverse.controller;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.PublicCategoryResponse;
import com.pcverse.service.CategoryService;
import com.pcverse.service.ProductCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryCatalogController {

    private final ProductCatalogService productCatalogService;
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<PaginationResponse<PublicCategoryResponse>> search(
            @Valid @ModelAttribute CategorySearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<PaginationResponse<PublicCategoryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(categoryService.searchForPublic(request, pageable))
                .build();
    }
}
