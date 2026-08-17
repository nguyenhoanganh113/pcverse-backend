package com.pcverse.service;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.response.AdminAttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import org.springframework.data.domain.Pageable;

public interface AttributeDefinitionService {

    AdminAttributeDefinitionResponse create(CreateAttributeDefinitionRequest request);

    PaginationResponse<AdminAttributeDefinitionResponse> searchForAdmin(AttributeDefinitionSearchRequest request, Pageable pageable);

    AdminAttributeDefinitionResponse getById(String id);

    AdminAttributeDefinitionResponse update(String id, UpdateAttributeDefinitionRequest request);

    void delete(String id, Long version);

    AdminAttributeDefinitionResponse updateStatus(
            String attributeDefinitionId,
            UpdateAttributeDefinitionStatusRequest request
    );

}
