package com.pcverse.service;

import com.pcverse.dto.request.*;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.AdminProductAttributesResponse;
import com.pcverse.dto.response.AdminProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    AdminProductResponse create(CreateProductRequest request);

    PaginationResponse<AdminProductResponse> searchForAdmin(AdminProductFilterRequest request, Pageable pageable);

    AdminProductResponse getById(String id);

    AdminProductAttributesResponse getAttributes(String id);

    AdminProductResponse update(String id, UpdateProductRequest request);

    AdminProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    AdminProductAttributesResponse updateAttributes(String id, UpdateProductAttributesRequest request);

    void delete(String id, Long version);

}
