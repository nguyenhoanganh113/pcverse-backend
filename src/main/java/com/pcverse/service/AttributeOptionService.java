package com.pcverse.service;

import com.pcverse.dto.request.AttributeOptionCreateRequest;
import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.AttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface AttributeOptionService {

    AttributeOptionResponse create(String attributeDefinitionId, AttributeOptionCreateRequest request);

    AttributeOptionResponse getById(String attributeDefinitionId, String attributeOptionId);

    PaginationResponse<AttributeOptionResponse> searchForAdmin(String attributeDefinitionId,
                                                               AttributeOptionSearchRequest request,
                                                               Pageable pageable);

    void delete(String attributeDefinitionId, String attributeOptionId, Long version);

    AttributeOptionResponse update(String attributeDefinitionId, String attributeOptionId,
                                   UpdateAttributeOptionRequest request);

    AttributeOptionResponse updateStatus(String attributeDefinitionId, String attributeOptionId, UpdateAttributeOptionStatusRequest request);

}
