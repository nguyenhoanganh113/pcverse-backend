package com.pcverse.service;

import com.pcverse.dto.request.CreateAddressRequest;
import com.pcverse.dto.request.UpdateAddressRequest;
import com.pcverse.dto.response.AddressResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface AddressService {

    List<AddressResponse> getMyAddresses(Jwt jwt);

    AddressResponse createMyAddress(Jwt jwt, CreateAddressRequest request);

    AddressResponse updateMyAddress(
            Jwt jwt,
            String addressId,
            UpdateAddressRequest request
    );

    void deleteMyAddress(Jwt jwt, String addressId);
}
