package com.pcverse.controller;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CategoryResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.CategoryService;
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

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_CREATE')")
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Category created successfully")
                .data(categoryService.create(request))
                .build();
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_VIEW')")
    public ApiResponse<CategoryResponse> getById(
            @PathVariable String categoryId
    ) {
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(categoryService.getById(categoryId))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_VIEW')")
    public ApiResponse<PaginationResponse<CategoryResponse>> search(
            @Valid @ModelAttribute CategorySearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<CategoryResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(categoryService.searchForAdmin(request, pageable))
                .build();
    }

    @PatchMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_UPDATE')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(categoryService.update(categoryId, request))
                .build();
    }

    @PatchMapping("/{categoryId}/status")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_UPDATE')")
    public ApiResponse<CategoryResponse> updateStatus(
            @PathVariable String categoryId,
            @Valid @RequestBody UpdateCategoryStatusRequest request
    ) {
        return ApiResponse.<CategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category status updated successfully")
                .data(categoryService.updateStatus(categoryId, request))
                .build();
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String categoryId,
            @RequestParam(required = false) Long version
    ) {
        categoryService.delete(categoryId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .build();
    }
}
