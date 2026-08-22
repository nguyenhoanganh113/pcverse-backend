package com.pcverse.service;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryStatusRequest;
import com.pcverse.dto.response.AdminCategoryResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.PublicCategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    AdminCategoryResponse create(CreateCategoryRequest request);

    PaginationResponse<AdminCategoryResponse> searchForAdmin(
            CategorySearchRequest request,
            Pageable pageable
    );

    PaginationResponse<PublicCategoryResponse> searchForPublic(
            CategorySearchRequest request,
            Pageable pageable
    );

    AdminCategoryResponse getById(String id);

    AdminCategoryResponse update(String id, UpdateCategoryRequest request);

    AdminCategoryResponse updateStatus(
            String id,
            UpdateCategoryStatusRequest request
    );

    void delete(String id, Long version);
}
