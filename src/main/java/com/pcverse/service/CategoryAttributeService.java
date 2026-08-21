package com.pcverse.service;

import com.pcverse.dto.request.BulkCreateCategoryAttributesRequest;
import com.pcverse.dto.request.CategoryAttributeSearchRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryAttributeService {

    AdminCategoryAttributeResponse getById(String categoryId, String categoryAttributeId);

    PaginationResponse<AdminCategoryAttributeResponse> searchForAdmin(
            String categoryId,
            CategoryAttributeSearchRequest request,
            Pageable pageable
    );

    AdminCategoryAttributeResponse update(
            String categoryId,
            String categoryAttributeId,
            UpdateCategoryAttributeRequest request
    );

    List<AdminCategoryAttributeResponse> createBulk(
            String categoryId,
            BulkCreateCategoryAttributesRequest request
    );

    void delete(
            String categoryId,
            String categoryAttributeId,
            Long version
    );

}
