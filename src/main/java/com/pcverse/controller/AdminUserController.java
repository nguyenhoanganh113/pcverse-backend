package com.pcverse.controller;

import com.pcverse.dto.request.AssignUserRoleRequest;
import com.pcverse.dto.request.AdminUserSearchRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.request.UpdateUserStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/search")
    ApiResponse<PaginationResponse<UserDetailsResponse>> getAllUsers(
            @Valid @ModelAttribute AdminUserSearchRequest searchRequest,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PaginationResponse<UserDetailsResponse> users =
                userService.getAllUsers(searchRequest, pageable);

        return ApiResponse.<PaginationResponse<UserDetailsResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get all users successfully")
                .data(users)
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserDetailsResponse> getUserById(@PathVariable String userId) {
        UserDetailsResponse data = userService.getUserById(userId);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User retrieved successfully")
                .data(data)
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        CreateUserResponse data = userService.createUser(request);

        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{userId}/roles")
    ApiResponse<UserDetailsResponse> assignRole(
            @PathVariable String userId,
            @RequestBody @Valid AssignUserRoleRequest request
    ) {
        UserDetailsResponse data = userService.assignRole(userId, request.roleName());

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Role assigned successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserDetailsResponse> updateUser(
            @PathVariable String userId,
            @RequestBody @Valid UpdateAdminUserRequest request
    ) {
        UserDetailsResponse data = userService.updateUser(userId, request);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User updated successfully")
                .data(data)
                .build();
    }

    @PatchMapping("/{userId}/status")
    ApiResponse<UserDetailsResponse> updateUserStatus(
            @PathVariable String userId,
            @RequestBody @Valid UpdateUserStatusRequest request
    ) {
        UserDetailsResponse data = userService.updateUserStatus(userId, request.status());

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User status updated successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User deleted successfully")
                .build();
    }

    @PutMapping("/{userId}/password")
    ApiResponse<Void> resetPassword(
            @PathVariable String userId,
            @RequestBody @Valid ResetUserPasswordRequest request
    ) {
        userService.resetPassword(userId, request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Password reset successfully")
                .build();
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    ApiResponse<UserDetailsResponse> removeRole(@PathVariable String userId,
                                                @PathVariable String roleName) {
        UserDetailsResponse data = userService.removeRole(userId, roleName);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Role removed successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{userId}/logout")
    ApiResponse<Void> logoutUser(@PathVariable String userId) {
        userService.logoutUser(userId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User logged out successfully")
                .build();
    }

    @GetMapping("/{userId}/sessions")
    ApiResponse<List<UserSessionResponse>> getUserSessions(@PathVariable String userId) {
        List<UserSessionResponse> data = userService.getUserSessions(userId);

        return ApiResponse.<List<UserSessionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("User sessions retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{userId}/credentials")
    ApiResponse<List<UserCredentialResponse>> getUserCredentials(@PathVariable String userId) {
        List<UserCredentialResponse> data = userService.getUserCredentials(userId);

        return ApiResponse.<List<UserCredentialResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("User credentials retrieved successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{userId}/credentials/{credentialId}")
    ApiResponse<Void> deleteUserCredential(
            @PathVariable String userId,
            @PathVariable String credentialId
    ) {
        userService.deleteUserCredential(userId, credentialId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User credential deleted successfully")
                .build();
    }

    @DeleteMapping("/{userId}/sessions/{sessionId}")
    ApiResponse<Void> terminateUserSession(
            @PathVariable String userId,
            @PathVariable String sessionId
    ) {
        userService.terminateUserSession(userId, sessionId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User session terminated successfully")
                .build();
    }

    @PostMapping("/{userId}/required-actions-email")
    ApiResponse<Void> sendRequiredActionsEmail(
            @PathVariable String userId,
            @RequestBody @Valid SendRequiredActionsEmailRequest request
    ) {
        userService.sendRequiredActionsEmail(userId, request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Required-actions email sent successfully")
                .build();
    }

    @PutMapping("/{userId}/required-actions")
    ApiResponse<Void> updateRequiredActions(
            @PathVariable String userId,
            @RequestBody @Valid UpdateUserRequiredActionsRequest request
    ) {
        userService.updateRequiredActions(userId, request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User required actions updated successfully")
                .build();
    }

}
