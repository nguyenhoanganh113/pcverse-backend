package com.pcverse.controller;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.AdminAttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.AttributeDefinitionService;
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
@RequestMapping("/api/v1/admin/attributes")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminAttributeDefinitionController {

    private final AttributeDefinitionService attributeDefinitionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_CREATE')")
    public ApiResponse<AdminAttributeDefinitionResponse> create(@Valid @RequestBody CreateAttributeDefinitionRequest request) {
        var data = attributeDefinitionService.create(request);
        return ApiResponse.<AdminAttributeDefinitionResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Attribute definition created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_VIEW')")
    public ApiResponse<PaginationResponse<AdminAttributeDefinitionResponse>> search(
            @Valid @ModelAttribute
            AttributeDefinitionSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ApiResponse
                .<PaginationResponse<AdminAttributeDefinitionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definitions retrieved successfully")
                .data(attributeDefinitionService.searchForAdmin(request, pageable))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_READ')")
    public ApiResponse<AdminAttributeDefinitionResponse> get(@PathVariable String id) {
        return ApiResponse
                .<AdminAttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition retrieved successfully")
                .data(attributeDefinitionService.getById(id))
                .build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_UPDATE')")
    public ApiResponse<AdminAttributeDefinitionResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateAttributeDefinitionRequest request
    ) {
        var data = attributeDefinitionService.update(id, request);
        return ApiResponse.<AdminAttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition updated successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{attributeDefinitionId}/status")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_UPDATE')")
    public ApiResponse<AdminAttributeDefinitionResponse> updateStatus(
            @PathVariable String attributeDefinitionId,
            @Valid @RequestBody UpdateAttributeDefinitionStatusRequest request
    ) {
        var data = attributeDefinitionService.updateStatus(attributeDefinitionId, request);
        return ApiResponse.<AdminAttributeDefinitionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition status updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_DEFINITION_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String id,
            @RequestParam @PositiveOrZero(message = "Version must be greater than or equal to 0") Long version
    ) {
        attributeDefinitionService.delete(id, version);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute definition deleted successfully")
                .build();
    }




}
