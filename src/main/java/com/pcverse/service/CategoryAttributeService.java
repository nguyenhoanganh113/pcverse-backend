package com.pcverse.service;

import com.pcverse.dto.request.CreateCategoryAttributeRequest;
import com.pcverse.dto.request.UpdateCategoryAttributeRequest;
import com.pcverse.dto.response.AdminCategoryAttributeResponse;

import java.util.List;

public interface CategoryAttributeService {

    AdminCategoryAttributeResponse create(String categoryId, CreateCategoryAttributeRequest request);

    AdminCategoryAttributeResponse getById(String categoryId, String categoryAttributeId);

    List<AdminCategoryAttributeResponse> getAllByCategoryId(String categoryId);

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
