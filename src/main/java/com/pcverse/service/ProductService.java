package com.pcverse.service;

import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.AdminProductSearchRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.request.UpdateProductAttributesRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductAttributesResponse;
import com.pcverse.dto.response.AdminProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    AdminProductResponse create(CreateProductRequest request);

    PaginationResponse<AdminProductResponse> searchForAdmin(
            AdminProductSearchRequest request,
            Pageable pageable
    );

    AdminProductResponse getById(String id);

    ProductAttributesResponse getAttributes(String id);

    AdminProductResponse update(String id, UpdateProductRequest request);

    AdminProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    ProductAttributesResponse updateAttributes(String id, UpdateProductAttributesRequest request);

    void delete(String id, Long version);

}
