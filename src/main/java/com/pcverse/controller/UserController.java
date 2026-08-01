package com.pcverse.controller;

import com.pcverse.dto.request.CreateAddressRequest;
import com.pcverse.dto.request.UpdateAddressRequest;
import com.pcverse.dto.request.UpdateMyProfileRequest;
import com.pcverse.dto.response.AddressResponse;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.service.AddressService;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@PreAuthorize("denyAll()") // Quên @PreAuthorize ở mỗi method thì sẽ có cái này catch
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PROFILE_READ_SELF')")
    ApiResponse<UserDetailsResponse> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        var data = userService.myInfo(jwt);
        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info retrieved successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PROFILE_UPDATE_SELF')")
    ApiResponse<UserDetailsResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateMyProfileRequest request
    ) {
        var data = userService.updateMyProfile(jwt, request);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Profile updated successfully")
                .data(data)
                .build();
    }

    @GetMapping("/me/sessions")
    @PreAuthorize("hasAuthority('ROLE_SESSION_READ_SELF')")
    ApiResponse<List<UserSessionResponse>> getMySessions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<UserSessionResponse> data = userService.getMySessions(jwt);

        return ApiResponse.<List<UserSessionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Sessions retrieved successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/me/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('ROLE_SESSION_TERMINATE_SELF')")
    ApiResponse<Void> terminateMySession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId
    ) {
        userService.terminateMySession(jwt, sessionId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Session terminated successfully")
                .build();
    }

    @GetMapping("/me/addresses")
    @PreAuthorize("hasAuthority('ROLE_ADDRESS_READ_SELF')")
    ApiResponse<List<AddressResponse>> getMyAddresses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        List<AddressResponse> data = addressService.getMyAddresses(jwt);

        return ApiResponse.<List<AddressResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Addresses retrieved successfully")
                .data(data)
                .build();
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADDRESS_CREATE_SELF')")
    ApiResponse<AddressResponse> createMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateAddressRequest request
    ) {
        AddressResponse data = addressService.createMyAddress(jwt, request);

        return ApiResponse.<AddressResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Address created successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/me/addresses/{addressId}")
    @PreAuthorize("hasAuthority('ROLE_ADDRESS_UPDATE_SELF')")
    ApiResponse<AddressResponse> updateMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId,
            @RequestBody @Valid UpdateAddressRequest request
    ) {
        AddressResponse data = addressService.updateMyAddress(
                jwt,
                addressId,
                request
        );

        return ApiResponse.<AddressResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Address updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @PreAuthorize("hasAuthority('ROLE_ADDRESS_DELETE_SELF')")
    ApiResponse<Void> deleteMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String addressId
    ) {
        addressService.deleteMyAddress(jwt, addressId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Address deleted successfully")
                .build();
    }
}
