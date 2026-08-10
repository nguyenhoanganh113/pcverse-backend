package com.pcverse.controller;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CategoryAttributeResponse;
import com.pcverse.service.CategoryAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories/{categoryId}/attributes")
@RequiredArgsConstructor
public class AdminCategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_CREATE')")
    public ApiResponse<CategoryAttributeResponse> create(
            @PathVariable String categoryId,
            @Valid @RequestBody
            CreateCategoryAttributeRequest request
    ) {
        CategoryAttributeResponse response = categoryAttributeService.create(categoryId, request);

        return ApiResponse
                .<CategoryAttributeResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message(
                        "Category attribute created successfully"
                )
                .data(response)
                .build();
    }

    @GetMapping("/{categoryAttributeId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_VIEW')")
    public ApiResponse<CategoryAttributeResponse> getById(
            @PathVariable String categoryId,
            @PathVariable String categoryAttributeId
    ) {
        return ApiResponse
                .<CategoryAttributeResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category attribute retrieved successfully")
                .data(categoryAttributeService.getById(categoryId, categoryAttributeId))
                .build();
    }

    @GetMapping
    @PreAuthorize(
            "hasAuthority('ROLE_CATEGORY_ATTRIBUTE_VIEW')"
    )
    public ApiResponse<List<CategoryAttributeResponse>> getAllByCategoryId(
            @PathVariable String categoryId
    ) {
        List<CategoryAttributeResponse> response = categoryAttributeService
                .getAllByCategoryId(categoryId);

        return ApiResponse
                .<List<CategoryAttributeResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Category attributes retrieved successfully")
                .data(response)
                .build();
    }

    @PatchMapping("/{categoryAttributeId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_UPDATE')")
    public ApiResponse<CategoryAttributeResponse> update(
            @PathVariable String categoryId,
            @PathVariable String categoryAttributeId,
            @Valid @RequestBody UpdateCategoryAttributeRequest request
    ) {
        CategoryAttributeResponse response =
                categoryAttributeService.update(
                        categoryId,
                        categoryAttributeId,
                        request
                );

        return ApiResponse
                .<CategoryAttributeResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category attribute updated successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/{categoryAttributeId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String categoryId,
            @PathVariable String categoryAttributeId,
            @RequestParam(required = false) Long version
    ) {
        categoryAttributeService.delete(
                categoryId,
                categoryAttributeId,
                version
        );

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Category attribute deleted successfully")
                .build();
    }

}
