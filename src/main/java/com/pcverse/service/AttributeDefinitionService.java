package com.pcverse.service;

import com.pcverse.dto.request.AttributeDefinitionSearchRequest;
import com.pcverse.dto.request.CreateAttributeDefinitionRequest;
import com.pcverse.dto.request.UpdateAttributeDefinitionRequest;
import com.pcverse.dto.response.AttributeDefinitionResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.request.UpdateAttributeDefinitionStatusRequest;
import org.springframework.data.domain.Pageable;

public interface AttributeDefinitionService {

    AttributeDefinitionResponse create(CreateAttributeDefinitionRequest request);

    PaginationResponse<AttributeDefinitionResponse> searchForAdmin(AttributeDefinitionSearchRequest request, Pageable pageable);

    AttributeDefinitionResponse getById(String id);

    AttributeDefinitionResponse update(String id, UpdateAttributeDefinitionRequest request);

    void delete(String id, Long version);

    AttributeDefinitionResponse updateStatus(
            String id,
            UpdateAttributeDefinitionStatusRequest request
    );

}
