package com.pcverse.controller;

import com.pcverse.dto.request.CompleteUserProfileRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    ApiResponse<UserDetailsResponse> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        var data = userService.myInfo(jwt);
        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info retrieved successfully")
                .data(data)
                .build();
    }

    @PostMapping("/me/complete-profile")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<UserDetailsResponse> completeMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CompleteUserProfileRequest request
    ) {
        UserDetailsResponse data = userService.completeProfile(jwt, request);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User profile completed successfully")
                .data(data)
                .build();
    }

}
