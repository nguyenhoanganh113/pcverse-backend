package com.pcverse.controller;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.CategoryAttributeSearchRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.CategoryAttributeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories/{categoryId}/attributes")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminCategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_CREATE')")
    public ApiResponse<AdminCategoryAttributeResponse> create(
            @PathVariable String categoryId,
            @Valid @RequestBody
            CreateCategoryAttributeRequest request
    ) {
        AdminCategoryAttributeResponse response = categoryAttributeService.create(categoryId, request);

        return ApiResponse
                .<AdminCategoryAttributeResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message(
                        "Category attribute created successfully"
                )
                .data(response)
                .build();
    }

    @GetMapping("/{categoryAttributeId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_READ')")
    public ApiResponse<AdminCategoryAttributeResponse> getById(
            @PathVariable String categoryId,
            @PathVariable String categoryAttributeId
    ) {
        return ApiResponse
                .<AdminCategoryAttributeResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category attribute retrieved successfully")
                .data(categoryAttributeService.getById(categoryId, categoryAttributeId))
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize(
            "hasAuthority('ROLE_CATEGORY_ATTRIBUTE_READ')"
    )
    public ApiResponse<PaginationResponse<AdminCategoryAttributeResponse>> searchForAdmin(
            @PathVariable String categoryId,
            @Valid @ModelAttribute CategoryAttributeSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = {"displayOrder", "id"},
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<AdminCategoryAttributeResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Category attributes retrieved successfully")
                .data(categoryAttributeService.searchForAdmin(
                        categoryId,
                        request,
                        pageable
                ))
                .build();
    }

    @PatchMapping("/{categoryAttributeId}")
    @PreAuthorize("hasAuthority('ROLE_CATEGORY_ATTRIBUTE_UPDATE')")
    public ApiResponse<AdminCategoryAttributeResponse> update(
            @PathVariable String categoryId,
            @PathVariable String categoryAttributeId,
            @Valid @RequestBody UpdateCategoryAttributeRequest request
    ) {
        AdminCategoryAttributeResponse response =
                categoryAttributeService.update(
                        categoryId,
                        categoryAttributeId,
                        request
                );

        return ApiResponse
                .<AdminCategoryAttributeResponse>builder()
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
            @RequestParam @PositiveOrZero(message = "Version must be greater than or equal to 0") Long version
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
