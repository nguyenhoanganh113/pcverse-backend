package com.pcverse.service;

import com.pcverse.dto.request.AdminBrandSearchRequest;
import com.pcverse.dto.request.CreateBrandRequest;
import com.pcverse.dto.request.UpdateBrandRequest;
import com.pcverse.dto.request.UpdateBrandStatusRequest;
import com.pcverse.dto.response.BrandResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface BrandService {

    BrandResponse create(CreateBrandRequest request);

    BrandResponse getById(String id);

    PaginationResponse<BrandResponse> searchForAdmin(AdminBrandSearchRequest request, Pageable pageable);

    BrandResponse update(String id, UpdateBrandRequest request);

    BrandResponse updateStatus(String id, UpdateBrandStatusRequest request);

    void delete(String id, Long version);


}
