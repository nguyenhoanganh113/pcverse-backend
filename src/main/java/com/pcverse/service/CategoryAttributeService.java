package com.pcverse.service;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.CategoryAttributeResponse;

import java.util.List;

public interface CategoryAttributeService {

    CategoryAttributeResponse create(String categoryId, CreateCategoryAttributeRequest request);

    CategoryAttributeResponse getById(String categoryId, String categoryAttributeId);

    List<CategoryAttributeResponse> getAllByCategoryId(String categoryId);

    CategoryAttributeResponse update(
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
