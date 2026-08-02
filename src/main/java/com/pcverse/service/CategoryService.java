package com.pcverse.service;

import com.pcverse.dto.request.CategorySearchRequest;
import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UpdateCategoryResponse;
import com.pcverse.dto.response.CategoryDetailResponse;
import com.pcverse.dto.response.CreateCategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CreateCategoryResponse createCategory(CreateCategoryRequest request);

    PaginationResponse<CategoryDetailResponse> searchCategories(CategorySearchRequest categorySearchRequest, Pageable pagable);

    CategoryDetailResponse getCategory(String id);

    UpdateCategoryResponse updateCategory(String id, UpdateCategoryRequest request);

    void deleteCategory(String id);

}
