package com.pcverse.service;

import com.pcverse.dto.request.AttributeOptionCreateRequest;
import com.pcverse.dto.request.AttributeOptionSearchRequest;
import com.pcverse.dto.request.UpdateAttributeOptionRequest;
import com.pcverse.dto.request.UpdateAttributeOptionStatusRequest;
import com.pcverse.dto.response.AdminAttributeOptionResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface AttributeOptionService {

    AdminAttributeOptionResponse create(String attributeDefinitionId, AttributeOptionCreateRequest request);

    AdminAttributeOptionResponse getById(String attributeDefinitionId, String attributeOptionId);

    PaginationResponse<AdminAttributeOptionResponse> searchForAdmin(String attributeDefinitionId,
                                                                    AttributeOptionSearchRequest request,
                                                                    Pageable pageable);

    void delete(String attributeDefinitionId, String attributeOptionId, Long version);

    AdminAttributeOptionResponse update(String attributeDefinitionId, String attributeOptionId,
                                        UpdateAttributeOptionRequest request);

    AdminAttributeOptionResponse updateStatus(String attributeDefinitionId, String attributeOptionId, UpdateAttributeOptionStatusRequest request);

}
