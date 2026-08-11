package com.pcverse.service;

import com.pcverse.dto.request.CreateProductRequest;
import com.pcverse.dto.request.AdminProductSearchRequest;
import com.pcverse.dto.request.UpdateProductRequest;
import com.pcverse.dto.request.UpdateProductAttributesRequest;
import com.pcverse.dto.request.UpdateProductStatusRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductAttributesResponse;
import com.pcverse.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    PaginationResponse<ProductResponse> searchForAdmin(
            AdminProductSearchRequest request,
            Pageable pageable
    );

    ProductResponse getById(String id);

    ProductResponse update(String id, UpdateProductRequest request);

    ProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    ProductAttributesResponse updateAttributes(
            String id,
            UpdateProductAttributesRequest request
    );

    void delete(String id, Long version);

}
