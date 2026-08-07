package com.pcverse.controller;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.AttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.AttributeDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/attributes")
@RequiredArgsConstructor
public class AdminAttributeDefinitionController {

    private final AttributeDefinitionService attributeDefinitionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_MANAGE')")
    public ApiResponse<AttributeDefinitionResponse> create(@Valid @RequestBody CreateAttributeDefinitionRequest request) {
        var data = attributeDefinitionService.create(request);
        return ApiResponse.<AttributeDefinitionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Attribute definition created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PaginationResponse<AttributeDefinitionResponse>> search(
            @Valid @ModelAttribute
            AttributeDefinitionSearchRequest request,
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<AttributeDefinitionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definitions retrieved successfully")
                .data(attributeDefinitionService.searchForAdmin(request, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AttributeDefinitionResponse> get(@PathVariable String id) {
        return ApiResponse
                .<AttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition retrieved successfully")
                .data(attributeDefinitionService.getById(id))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<AttributeDefinitionResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateAttributeDefinitionRequest request
    ) {
        var data = attributeDefinitionService.update(id, request);
        return ApiResponse.<AttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition updated successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AttributeDefinitionResponse> updateStatus(
            @PathVariable String id,
            @RequestParam UpdateAttributeDefinitionStatusRequest request
    ) {
        var data = attributeDefinitionService.updateStatus(id, request);
        return ApiResponse.<AttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition status updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        attributeDefinitionService.delete(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.NO_CONTENT.value())
                .message("Attribute definition deleted successfully")
                .build();
    }




}
