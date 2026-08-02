package com.pcverse.controller;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.response.*;
import com.pcverse.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
@Validated
@PreAuthorize("denyAll()")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CreateCategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        var data = categoryService.createCategory(request);
        return ApiResponse.<CreateCategoryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Category created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_VIEW')")
    ApiResponse<PaginationResponse<CategoryDetailResponse>> searchCategory(
            @ModelAttribute CategorySearchRequest categorySearchRequest,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        var data = categoryService.searchCategories(categorySearchRequest, pageable);
        return ApiResponse.<PaginationResponse<CategoryDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_READ')")
    ApiResponse<CategoryDetailResponse> getCategory(@PathVariable String id) {
        var data = categoryService.getCategory(id);

        return ApiResponse.<CategoryDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(data)
                .build();
    }

    @PatchMapping(value = "/{id}", consumes = "application/merge-patch+json")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_UPDATE')")
    ApiResponse<UpdateCategoryResponse> updateCategory(
            @PathVariable String id,
            @RequestBody @Valid UpdateCategoryRequest request
    ) {
        var data = categoryService.updateCategory(id, request);
        return ApiResponse.<UpdateCategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_DELETE')")
    ApiResponse<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .build();
    }

}
