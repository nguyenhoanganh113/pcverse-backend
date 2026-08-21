package com.pcverse.service;

import com.pcverse.dto.request.*;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.AdminProductAttributesResponse;
import com.pcverse.dto.response.AdminProductConfigurationResponse;
import com.pcverse.dto.response.AdminProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    AdminProductResponse create(CreateProductRequest request);

    PaginationResponse<AdminProductResponse> searchForAdmin(AdminProductFilterRequest request, Pageable pageable);

    AdminProductResponse getById(String id);

    AdminProductAttributesResponse getAttributes(String id);

    AdminProductConfigurationResponse updateConfiguration(
            String id,
            UpdateProductConfigurationRequest request
    );

    AdminProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    void delete(String id, Long version);

}
