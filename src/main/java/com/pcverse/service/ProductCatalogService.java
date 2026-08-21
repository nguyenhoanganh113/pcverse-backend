package com.pcverse.service;

import com.pcverse.dto.request.ProductSearchRequest;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.ProductDetailResponse;
import com.pcverse.dto.response.ProductFiltersResponse;
import com.pcverse.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface ProductCatalogService {

    PaginationResponse<ProductSummaryResponse> search(
            ProductSearchRequest request,
            Pageable pageable
    );

    ProductDetailResponse getBySlug(String slug);

    ProductFiltersResponse getFilters(String categoryId);
}
