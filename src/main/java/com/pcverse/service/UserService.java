package com.pcverse.service;

import com.pcverse.dto.request.AdminUserSearchRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateMyProfileRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.response.AdminUserResponse;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailsResponse myInfo(Jwt jwt);

    UserDetailsResponse updateMyProfile(Jwt jwt, UpdateMyProfileRequest request);

    List<UserSessionResponse> getMySessions(Jwt jwt);

    void terminateMySession(Jwt jwt, String sessionId);

    PaginationResponse<AdminUserResponse> searchUsers(AdminUserSearchRequest searchRequest, Pageable pageable);

    AdminUserResponse getUserById(String userId);

    AdminUserResponse updateUserStatus(String userId, UserStatus status);

    AdminUserResponse updateUser(String userId, UpdateAdminUserRequest request);

    void deleteUser(String userId);

    void resetPassword(String userId, ResetUserPasswordRequest request);

    AdminUserResponse assignRole(String userId, String roleName);

    User ensureUserExistsFromToken(Jwt jwt);

    AdminUserResponse removeRole(String userId, String roleName);

    void logoutUser(String userId);

    List<UserSessionResponse> getUserSessions(String userId);

    void terminateUserSession(String userId, String sessionId);

    void sendRequiredActionsEmail(
            String userId,
            SendRequiredActionsEmailRequest request
    );

    void updateRequiredActions(
            String userId,
            UpdateUserRequiredActionsRequest request
    );

}
