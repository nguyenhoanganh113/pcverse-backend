package com.pcverse.controller;

import com.pcverse.dto.request.AttributeOptionCreateRequest;
import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.AttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.AttributeOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/attributes/{attributeDefinitionId}/options")
@RequiredArgsConstructor
public class AdminAttributeOptionController {

    private final AttributeOptionService attributeOptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_CREATE')")
    public ApiResponse<AttributeOptionResponse> create(
            @PathVariable String attributeDefinitionId,
            @Valid @RequestBody AttributeOptionCreateRequest request
    ) {
        AttributeOptionResponse response =
                attributeOptionService.create(attributeDefinitionId, request);

        return ApiResponse.<AttributeOptionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Attribute option created successfully")
                .data(response)
                .build();
    }

    @GetMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_VIEW')")
    public ApiResponse<AttributeOptionResponse> getById(
            @PathVariable String attributeDefinitionId,
            @PathVariable String attributeOptionId
    ) {
        AttributeOptionResponse response = attributeOptionService.getById(attributeDefinitionId, attributeOptionId);

        return ApiResponse.<AttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_VIEW')")
    public ApiResponse<PaginationResponse<AttributeOptionResponse>> searchForAdmin(
            @PathVariable String attributeDefinitionId,
            @ModelAttribute AttributeOptionSearchRequest request,
            Pageable pageable) {

        return ApiResponse.<PaginationResponse<AttributeOptionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute options retrieved successfully")
                .data(
                        attributeOptionService.searchForAdmin(
                                attributeDefinitionId,
                                request,
                                pageable
                        )
                )
                .build();
    }

    @DeleteMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String attributeDefinitionId,
            @PathVariable String attributeOptionId,
            @RequestParam Long version
    ) {
        attributeOptionService.delete(attributeDefinitionId, attributeOptionId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option deleted successfully")
                .build();
    }

    @PatchMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_UPDATE')")
    public ApiResponse<AttributeOptionResponse> update(
            @PathVariable String attributeDefinitionId,
            @PathVariable String attributeOptionId,
            @Valid @RequestBody UpdateAttributeOptionRequest request
    ) {

        AttributeOptionResponse response =
                attributeOptionService.update(
                        attributeDefinitionId,
                        attributeOptionId,
                        request
                );

        return ApiResponse.<AttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option updated successfully")
                .data(response)
                .build();
    }


    @PatchMapping("/{attributeOptionId}/status")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_UPDATE')")
    public ApiResponse<AttributeOptionResponse> updateStatus(
            @PathVariable String attributeDefinitionId,
            @PathVariable String attributeOptionId,
            @Valid @RequestBody UpdateAttributeOptionStatusRequest request
    ) {

        AttributeOptionResponse response =
                attributeOptionService.updateStatus(
                        attributeDefinitionId,
                        attributeOptionId,
                        request
                );

        return ApiResponse.<AttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option status updated successfully")
                .data(response)
                .build();
    }




}
