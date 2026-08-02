package com.pcverse.controller;

import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryResponse;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CategoryDetailResponse;
import com.pcverse.dto.response.CreateCategoryResponse;
import com.pcverse.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
@Validated
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

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_READ')")
    ApiResponse<List<CategoryDetailResponse>> getCategories(
            @RequestParam(defaultValue = "true") boolean active
    ) {
        var data = categoryService.getCategories(active);
        return ApiResponse.<List<CategoryDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
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
