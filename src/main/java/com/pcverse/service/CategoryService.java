package com.pcverse.service;

import com.pcverse.dto.request.CreateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryRequest;
import com.pcverse.dto.request.UpdateCategoryResponse;
import com.pcverse.dto.response.CategoryDetailResponse;
import com.pcverse.dto.response.CreateCategoryResponse;

import java.util.List;

public interface CategoryService {

    CreateCategoryResponse createCategory(CreateCategoryRequest request);

    List<CategoryDetailResponse> getCategories(boolean active);

    UpdateCategoryResponse updateCategory(String id, UpdateCategoryRequest request);

    void deleteCategory(String id);

}
