package com.pcverse.controller;

import com.pcverse.dto.request.UpdateMyProfileRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@PreAuthorize("denyAll()") // Quên @PreAuthorize ở mỗi method thì sẽ có cái này catch
public class UserController {

    private final UserService userService;

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
}
