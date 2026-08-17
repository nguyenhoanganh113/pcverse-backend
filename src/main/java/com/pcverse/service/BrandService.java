package com.pcverse.service;

import com.pcverse.dto.request.AdminBrandSearchRequest;
import com.pcverse.dto.request.CreateBrandRequest;
import com.pcverse.dto.request.UpdateBrandRequest;
import com.pcverse.dto.request.UpdateBrandStatusRequest;
import com.pcverse.dto.response.AdminBrandResponse;
import com.pcverse.dto.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface BrandService {

    AdminBrandResponse create(CreateBrandRequest request);

    AdminBrandResponse getById(String id);

    PaginationResponse<AdminBrandResponse> searchForAdmin(AdminBrandSearchRequest request, Pageable pageable);

    AdminBrandResponse update(String id, UpdateBrandRequest request);

    AdminBrandResponse updateStatus(String id, UpdateBrandStatusRequest request);

    void delete(String id, Long version);


}
