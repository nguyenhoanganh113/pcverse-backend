package com.pcverse.service;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryStatusRequest;
import com.pcverse.dto.response.CategoryResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse create(CreateCategoryRequest request);

    PaginationResponse<CategoryResponse> searchForAdmin(
            CategorySearchRequest request,
            Pageable pageable
    );

    CategoryResponse getById(String id);

    CategoryResponse update(String id, UpdateCategoryRequest request);

    CategoryResponse updateStatus(
            String id,
            UpdateCategoryStatusRequest request
    );

    void delete(String id, Long version);
}
