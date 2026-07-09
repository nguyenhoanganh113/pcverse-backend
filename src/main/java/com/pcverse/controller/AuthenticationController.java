package com.pcverse.controller;

import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.ExchangeTokenResponse;
import com.pcverse.dto.response.LoginResponse;
import com.pcverse.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {

        var data = authenticationService.login(request);

        // Lưu refresh token vào cookie
        Cookie cookie = new Cookie("refresh-token", data.refreshToken());
        cookie.setHttpOnly(true); //Prevents JavaScript from accessing the cookie (XSS protection)
        cookie.setSecure(false); // Change to true in productio
        cookie.setPath("/"); // Cookie is accessible across all paths in the app
        cookie.setMaxAge(14 * 24 * 60 * 60); // Cookie expiry: 14 days — matches refresh token TTL
        response.addCookie(cookie);

        // Tạo response mới không chứa refresh token
        LoginResponse responseData = LoginResponse.builder()
                .accessToken(data.accessToken())
                .refreshToken(null) // Không trả về refresh token
                .roles(data.roles())
                .build();

        return ApiResponse.<LoginResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Login successful")
                .data(responseData)
                .build();
    }

    @PostMapping("/refresh-token")
    ApiResponse<LoginResponse> refreshToken(@CookieValue("refresh-token") String refreshToken) {

        ExchangeTokenResponse data = authenticationService.refreshToken(refreshToken);

        LoginResponse responseData = LoginResponse.builder()
                .accessToken(data.accessToken())
                .refreshToken(null)
                .roles(data.roles())
                .build();

        return ApiResponse.<LoginResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Refresh token successful")
                .data(responseData)
                .build();

    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue("refresh-token") String refreshToken,
            HttpServletResponse response
    ) {
        // 1. Gọi service để thu hồi tokens
        authenticationService.logout(refreshToken);

        // 2. Xóa refresh token cookie
        // Set value = "" và maxAge = 0 để browser xóa cookie
        Cookie cookie = new Cookie("refresh-token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(0); // Xóa cookie ngay lập tức

        response.addCookie(cookie);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }

}
