package com.pcverse.service;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.CategoryAttributeSearchRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryAttributeService {

    AdminCategoryAttributeResponse create(String categoryId, CreateCategoryAttributeRequest request);

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

    void delete(
            String categoryId,
            String categoryAttributeId,
            Long version
    );

}
