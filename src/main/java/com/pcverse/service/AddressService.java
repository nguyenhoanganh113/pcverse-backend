package com.pcverse.service;

import com.pcverse.dto.request.CreateAddressRequest;
import com.pcverse.dto.request.UpdateAddressRequest;
import com.pcverse.dto.response.AddressResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getMyAddresses(Jwt jwt);

    AddressResponse createMyAddress(Jwt jwt, CreateAddressRequest request);

    AddressResponse updateMyAddress(
            Jwt jwt,
            UUID addressId,
            UpdateAddressRequest request
    );

    void deleteMyAddress(Jwt jwt, UUID addressId);
}
