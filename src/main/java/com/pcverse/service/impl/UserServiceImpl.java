package com.pcverse.service.impl;

import com.pcverse.dto.request.AdminUserSearchRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateMyProfileRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.Role;
import com.pcverse.entity.User;
import com.pcverse.entity.UserHasRole;
import com.pcverse.enums.UserStatus;
import com.pcverse.event.UserDeletedEvent;
import com.pcverse.event.UserEmailChangedEvent;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.UserMapper;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.KeycloakAdminService;
import com.pcverse.service.KeycloakEmailService;
import com.pcverse.service.RedisTokenService;
import com.pcverse.service.RoleService;
import com.pcverse.service.UserService;
import com.pcverse.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakEmailService keycloakEmailService;
    private final RedisTokenService redisTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsernameIgnoreCase(request.username()) ||
                userRepository.existsByEmailIgnoreCase(request.email())) {

            log.error("Username or Email already exists when admin create a user");
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);

        }

        // Credentials are managed by Keycloak and are not stored in the local user.
        User user = userMapper.toUser(request);
        user.setUserStatus(UserStatus.PENDING_VERIFICATION);

        String keycloakUserId = keycloakAdminService.createUser(request);

        try {
            Role customerRole = roleService.getRoleByName("CUSTOMER");

            user.setKeycloakId(keycloakUserId);
            user.addRole(customerRole);
            userRepository.saveAndFlush(user);

            keycloakEmailService.sendVerifyEmailAsync(keycloakUserId, request.username());

            log.info("User created with Keycloak ID {}", keycloakUserId);
        } catch (RuntimeException exception) {

            try {
                keycloakAdminService.deleteUser(keycloakUserId);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error(
                        "Failed to remove Keycloak user {} after create-user workflow failed",
                        keycloakUserId,
                        cleanupException
                );
            }

            if (exception instanceof DataIntegrityViolationException) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            throw exception;
        }

        // Convert Entity sang Response DTO
        return userMapper.toCreateUserResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse myInfo(Jwt jwt) {

        User user = ensureUserExistsFromToken(jwt);

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse updateMyProfile(
            Jwt jwt,
            UpdateMyProfileRequest request
    ) {
        User user = ensureUserExistsFromToken(jwt);
        String keycloakId = requireKeycloakId(user);

        boolean keycloakProfileChanged = false;

        if (request.firstName() != null) {
            keycloakProfileChanged = !Objects.equals(
                    user.getFirstName(),
                    request.firstName()
            );
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            keycloakProfileChanged = keycloakProfileChanged || !Objects.equals(
                    user.getLastName(),
                    request.lastName()
            );
            user.setLastName(request.lastName());
        }

        if (request.phoneNumber() != null) {
            user.setPhoneNumber(nullIfBlank(request.phoneNumber()));
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            user.setDateOfBirth(request.dateOfBirth());
        }
        if (request.urlAvatar() != null) {
            user.setUrlAvatar(nullIfBlank(request.urlAvatar()));
        }

        User updatedUser = userRepository.saveAndFlush(user);

        if (keycloakProfileChanged) {
            keycloakAdminService.updateUserProfile(
                    keycloakId,
                    updatedUser.getFirstName(),
                    updatedUser.getLastName()
            );
        }

        return userMapper.toUserDetailResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<UserDetailsResponse> getAllUsers(AdminUserSearchRequest searchRequest, Pageable pageable) {

        // Lấy danh sách user của page hiện tại theo các điều kiện tìm kiếm
        Page<User> userPage = userRepository.findAll(UserSpecification.filter(searchRequest), pageable);

        if (userPage.isEmpty()) {
            return toPaginationResponse(userPage, List.of());
        }

        // Lấy tất cả userId trong page hiện tại
        List<String> userIds = userPage.getContent()
                .stream()
                .map(User::getId)
                .toList();

        Map<String, User> usersWithRolesById =
                userRepository.findAllWithRolesByIdIn(userIds)
                        .stream()
                        .collect(Collectors.toMap(
                                User::getId,
                                Function.identity()
                        ));

        List<UserDetailsResponse> users = userPage.getContent()
                .stream()
                .map(user -> usersWithRolesById.getOrDefault(
                        user.getId(),
                        user
                ))
                .map(userMapper::toUserDetailResponse)
                .toList();

        return toPaginationResponse(userPage, users);
    }

    private PaginationResponse<UserDetailsResponse> toPaginationResponse(
            Page<User> userPage,
            List<UserDetailsResponse> users
    ) {
        return PaginationResponse.<UserDetailsResponse>builder()
                .currentPage(userPage.getNumber())
                .size(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .data(users)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponse getUserById(String userId) {
        User user = userRepository.findDetailsById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse updateUserStatus(String userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);

        boolean enabled = switch (status) {
            case ACTIVE -> true;
            case DISABLED -> false;
            case LOCKED, PENDING_VERIFICATION, DELETED ->
                    throw new AppException(ErrorCode.USER_STATUS_NOT_SUPPORTED);
        };

        user.setUserStatus(status);
        userRepository.saveAndFlush(user);

        keycloakAdminService.updateUserEnabledStatus(
                keycloakId,
                enabled
        );

        if (status == UserStatus.DISABLED) {
            logoutAndRevokeTokens(keycloakId);
        }

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse updateUser(String userId, UpdateAdminUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.email());
        boolean tokenClaimsChanged = emailChanged
                || !Objects.equals(user.getFirstName(), request.firstName())
                || !Objects.equals(user.getLastName(), request.lastName());

        boolean emailExists = emailChanged
                && userRepository.existsByEmailIgnoreCase(request.email());

        if (emailExists) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setUrlAvatar(request.urlAvatar());

        if (emailChanged && user.getUserStatus() == UserStatus.ACTIVE) {
            user.setUserStatus(UserStatus.PENDING_VERIFICATION);
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        keycloakAdminService.updateUser(keycloakId, request);

        if (tokenClaimsChanged) {
            logoutAndRevokeTokens(keycloakId);
        }

        if (emailChanged) {
            eventPublisher.publishEvent(new UserEmailChangedEvent(
                    keycloakId,
                    user.getUsername()
            ));
        }

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String keycloakUserId = requireKeycloakId(user);
        String email = user.getEmail();
        String username = user.getUsername();
        boolean newlyDeleted = user.getUserStatus() != UserStatus.DELETED;

        if (newlyDeleted) {
            user.markDeleted(Instant.now());
            userRepository.saveAndFlush(user);
        }

        boolean keycloakUserDeleted =
                keycloakAdminService.deleteUser(keycloakUserId);
        redisTokenService.revokeAllUserTokens(keycloakUserId);

        if (newlyDeleted || keycloakUserDeleted) {
            eventPublisher.publishEvent(new UserDeletedEvent(
                    email,
                    username
            ));
        }
    }

    @Override
    public void resetPassword(String userId, ResetUserPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);

        keycloakAdminService.resetPassword(
                keycloakId,
                request.newPassword(),
                request.isTemporary()
        );
        logoutAndRevokeTokens(keycloakId);
    }

    @Override
    @Transactional
    public UserDetailsResponse assignRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);

        boolean alreadyAssigned = user.getUserHasRoles().stream()
                .anyMatch(userRole -> roleName.equalsIgnoreCase(userRole.getRole().getRoleName()));
        if (!alreadyAssigned) {
            user.addRole(roleService.getRoleByName(roleName));
            userRepository.saveAndFlush(user);
        }

        keycloakAdminService.assignRealmRole(keycloakId, roleName);
        return userMapper.toUserDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public User ensureUserExistsFromToken(Jwt jwt) {

        // Lấy keycloakId chính là "sub" thuộc claims
        String keycloakId = requiredClaim(jwt.getSubject());

        // Lấy "email" thuộc claims
        String email = normalizedEmail(jwt);

        // Lấy "preferred_username" thuộc claims chính là username của User
        String username = requiredClaim(jwt.getClaimAsString("preferred_username"));

        // Nếu email chưa được verify
        requireVerifiedEmail(jwt);

        return userRepository.findByKeycloakId(keycloakId) // Account Linking tự động theo keycloakId
                .map(existingUser -> {
                    activateUserAfterEmailVerification(existingUser);
                    return syncIdentityFromToken(existingUser, username, email);
                })
                // Account Linking tự động theo Email
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email)
                        .map(existingUser -> { // Nếu tìm thấy bằng email (ignoreCase)
                            // Cập nhật status user từ PENDING -> ACTIVE
                            activateUserAfterEmailVerification(existingUser);
                            User linkedUser = linkExistingUser(existingUser, keycloakId, username);
                            return syncIdentityFromToken(linkedUser, username, email);
                        })
                        .orElseGet(() -> createLocalUserFromToken(jwt, keycloakId, username, email)));
    }

    @Override
    @Transactional
    public UserDetailsResponse removeRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);

        UserHasRole assignedRole = user.getUserHasRoles().stream()
                .filter(userRole ->
                        roleName.equalsIgnoreCase(
                                userRole.getRole().getRoleName()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AppException(ErrorCode.USER_ROLE_NOT_ASSIGNED)
                );

        String actualRoleName = assignedRole.getRole().getRoleName();

        user.getUserHasRoles().remove(assignedRole);
        User updatedUser = userRepository.saveAndFlush(user);

        keycloakAdminService.removeRealmRole(
                keycloakId,
                actualRoleName
        );

        // Gỡ role khỏi Keycloak trước để mọi token được cấp mới không còn role cũ,
        // sau đó thu hồi session và các access token đã được cấp trước đó.
        logoutAndRevokeTokens(keycloakId);

        return userMapper.toUserDetailResponse(updatedUser);
    }

    @Override
    public void logoutUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        logoutAndRevokeTokens(requireKeycloakId(user));
    }

    @Override
    public List<UserSessionResponse> getUserSessions(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        return keycloakAdminService.getUserSessions(
                requireKeycloakId(user)
        );
    }

    @Override
    public void terminateUserSession(String userId, String sessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        String keycloakId = requireKeycloakId(user);
        boolean sessionBelongsToUser = keycloakAdminService.getUserSessions(keycloakId)
                .stream()
                .anyMatch(session -> sessionId.equals(session.sessionId()));

        if (!sessionBelongsToUser) {
            throw new AppException(ErrorCode.USER_SESSION_NOT_FOUND);
        }

        keycloakAdminService.deleteUserSession(sessionId);
        redisTokenService.revokeUserSession(sessionId);
    }

    @Override
    public void sendRequiredActionsEmail(
            String userId,
            SendRequiredActionsEmailRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        keycloakAdminService.sendRequiredActionsEmail(
                requireKeycloakId(user),
                request.actions(),
                request.resolvedLifespanSeconds()
        );
    }

    @Override
    public void updateRequiredActions(
            String userId,
            UpdateUserRequiredActionsRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        requireUserNotDeleted(user);

        keycloakAdminService.updateRequiredActions(
                requireKeycloakId(user),
                request.actions()
        );
    }

    private User linkExistingUser(User user, String keycloakId, String username) {

        // User này đã link với keycloakId khác trong keycloak database
        if (user.getKeycloakId() != null && !user.getKeycloakId().equals(keycloakId)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        // set keycloakId để user trong keycloak database
        user.setKeycloakId(keycloakId);

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            // Chắc chắn trong database không có username nào khác giống với username trong keycloak database
            validateUsernameAvailable(username);
            user.setUsername(username);
        }

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private User syncIdentityFromToken(User user, String username, String email) {
        user.setEmail(email);
        user.setUsername(username);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private User createLocalUserFromToken(Jwt jwt, String keycloakId, String username, String email) {

        // Nếu có user có username trùng thì throw exception
        validateUsernameAvailable(username);

        Role customerRole = roleService.getRoleByName("CUSTOMER");

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(requiredClaim(jwt.getClaimAsString("given_name")))
                .lastName(requiredClaim(jwt.getClaimAsString("family_name")))
                .urlAvatar(jwt.getClaimAsString("picture"))
                .userStatus(UserStatus.ACTIVE)
                .build();

        user.addRole(customerRole);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private void validateUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private void requireActiveUser(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_ACCOUNT_INACTIVE);
        }
    }

    private void requireUserNotDeleted(User user) {
        if (user.getUserStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.USER_ACCOUNT_INACTIVE);
        }
    }

    private void activateUserAfterEmailVerification(User user) {
        if (user.getUserStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setUserStatus(UserStatus.ACTIVE);
            return;
        }

        requireActiveUser(user);
    }

    private String requireKeycloakId(User user) {
        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            throw new AppException(ErrorCode.KEYCLOAK_USER_NOT_LINKED);
        }
        return user.getKeycloakId();
    }

    private void logoutAndRevokeTokens(String keycloakUserId) {
        keycloakAdminService.logoutUser(keycloakUserId);
        redisTokenService.revokeAllUserTokens(keycloakUserId);
    }

    private String normalizedEmail(Jwt jwt) {
        return requiredClaim(jwt.getClaimAsString("email"))
                .toLowerCase(Locale.ROOT);
    }

    private void requireVerifiedEmail(Jwt jwt) {
        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    private String requiredClaim(String claims){
        if (claims == null || claims.isBlank()){
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return claims.strip();
    }

    private String nullIfBlank(String value) {
        return value.isBlank() ? null : value;
    }
}
