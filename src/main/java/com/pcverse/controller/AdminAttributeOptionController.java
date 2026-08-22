package com.pcverse.controller;

import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.BulkCreateAttributeOptionsRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.AdminAttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.service.AttributeOptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/attribute-definitions/{definitionId}/options")
@RequiredArgsConstructor
@PreAuthorize("denyAll()")
public class AdminAttributeOptionController {

    private final AttributeOptionService attributeOptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_CREATE')")
    public ApiResponse<List<AdminAttributeOptionResponse>> createBulk(
            @PathVariable String definitionId,
            @Valid @RequestBody BulkCreateAttributeOptionsRequest request
    ) {
        return ApiResponse.<List<AdminAttributeOptionResponse>>builder()
                .code(HttpStatus.CREATED.value())
                .message("Attribute options created successfully")
                .data(attributeOptionService.createBulk(
                        definitionId,
                        request
                ))
                .build();
    }

    @GetMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_READ')")
    public ApiResponse<AdminAttributeOptionResponse> getById(
            @PathVariable String definitionId,
            @PathVariable String attributeOptionId
    ) {
        AdminAttributeOptionResponse response = attributeOptionService.getById(
                definitionId,
                attributeOptionId
        );

        return ApiResponse.<AdminAttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_READ')")
    public ApiResponse<PaginationResponse<AdminAttributeOptionResponse>> searchForAdmin(
            @PathVariable String definitionId,
            @Valid @ModelAttribute AttributeOptionSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = {"displayOrder", "id"},
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return ApiResponse.<PaginationResponse<AdminAttributeOptionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute options retrieved successfully")
                .data(attributeOptionService.searchForAdmin(
                        definitionId,
                        request,
                        pageable
                ))
                .build();
    }

    @DeleteMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_DELETE')")
    public ApiResponse<Void> delete(
            @PathVariable String definitionId,
            @PathVariable String attributeOptionId,
            @RequestParam @PositiveOrZero(message = "Version must be greater than or equal to 0") Long version
    ) {
        attributeOptionService.delete(definitionId, attributeOptionId, version);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option deleted successfully")
                .build();
    }

    @PatchMapping("/{attributeOptionId}")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_UPDATE')")
    public ApiResponse<AdminAttributeOptionResponse> update(
            @PathVariable String definitionId,
            @PathVariable String attributeOptionId,
            @Valid @RequestBody UpdateAttributeOptionRequest request
    ) {

        AdminAttributeOptionResponse response =
                attributeOptionService.update(
                        definitionId,
                        attributeOptionId,
                        request
                );

        return ApiResponse.<AdminAttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option updated successfully")
                .data(response)
                .build();
    }


    @PatchMapping("/{attributeOptionId}/status")
    @PreAuthorize("hasAuthority('ROLE_ATTRIBUTE_OPTION_UPDATE')")
    public ApiResponse<AdminAttributeOptionResponse> updateStatus(
            @PathVariable String definitionId,
            @PathVariable String attributeOptionId,
            @Valid @RequestBody UpdateAttributeOptionStatusRequest request
    ) {

        AdminAttributeOptionResponse response =
                attributeOptionService.updateStatus(
                        definitionId,
                        attributeOptionId,
                        request
                );

        return ApiResponse.<AdminAttributeOptionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Attribute option status updated successfully")
                .data(response)
                .build();
    }




}
