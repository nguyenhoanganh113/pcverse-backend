package com.pcverse.service.impl;

import com.pcverse.dto.request.AdminUserSearchRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.Role;
import com.pcverse.entity.User;
import com.pcverse.entity.UserHasRole;
import com.pcverse.enums.UserStatus;
import com.pcverse.event.UserCreatedEvent;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.UserMapper;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.RoleService;
import com.pcverse.service.KeycloakAdminService;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final Set<String> DELETABLE_CREDENTIAL_TYPES = Set.of(
            "otp",
            "webauthn",
            "webauthn-passwordless"
    );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final KeycloakAdminService keycloakAdminService;
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
        user.setUserStatus(UserStatus.ACTIVE);

        String keycloakUserId = keycloakAdminService.createUser(request);

        Role customerRole = roleService.getRoleByName("CUSTOMER");
        keycloakAdminService.assignClientRole(keycloakUserId, "CUSTOMER");

        user.setKeycloakId(keycloakUserId);
        user.addRole(customerRole);

        try {
            userRepository.saveAndFlush(user);

            eventPublisher.publishEvent(new UserCreatedEvent(keycloakUserId, request.username()));

            log.info("User created with Keycloak ID {}", keycloakUserId);
        } catch (RuntimeException exception) {

            try {
                keycloakAdminService.deleteUser(keycloakUserId);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error(
                        "Failed to remove Keycloak user {} after local save failed",
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
        String keycloakId = requireKeycloakId(user);

        boolean enabled = switch (status) {
            case ACTIVE -> true;
            case DISABLED -> false;
            case LOCKED, PENDING_VERIFICATION ->
                    throw new AppException(ErrorCode.USER_STATUS_NOT_SUPPORTED);
        };

        user.setUserStatus(status);
        userRepository.saveAndFlush(user);

        keycloakAdminService.updateUserEnabledStatus(
                keycloakId,
                enabled
        );

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse updateUser(String userId, UpdateAdminUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            throw new AppException(ErrorCode.KEYCLOAK_USER_NOT_LINKED);
        }
        String keycloakId = user.getKeycloakId();

        boolean emailExists = !user.getEmail().equalsIgnoreCase(request.email())
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

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        keycloakAdminService.updateUser(keycloakId, request);
        return userMapper.toUserDetailResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        keycloakAdminService.deleteUser(requireKeycloakId(user));
        userRepository.delete(user);
        userRepository.flush();
    }

    @Override
    public void resetPassword(String userId, ResetUserPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        keycloakAdminService.resetPassword(
                requireKeycloakId(user),
                request.newPassword(),
                request.isTemporary()
        );
    }

    @Override
    @Transactional
    public UserDetailsResponse assignRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            throw new AppException(ErrorCode.KEYCLOAK_USER_NOT_LINKED);
        }
        String keycloakId = user.getKeycloakId();

        boolean alreadyAssigned = user.getUserHasRoles().stream()
                .anyMatch(userRole -> roleName.equalsIgnoreCase(userRole.getRole().getRoleName()));
        if (!alreadyAssigned) {
            user.addRole(roleService.createRole(roleName));
            userRepository.saveAndFlush(user);
        }

        keycloakAdminService.assignClientRole(keycloakId, roleName);
        return userMapper.toUserDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public User ensureUserExistsFromToken(Jwt jwt) {

        // Lấy keycloakId chính là "sub" thuộc claims
        String keycloakId = requiredClaim(jwt.getSubject());

        // Lấy "email" thuộc claims
        String email = requiredClaim(Objects.requireNonNull(jwt.getClaimAsString("email")).toLowerCase(Locale.ROOT));

        // Lấy "preferred_username" thuộc claims chính là username của User
        String username = requiredClaim(jwt.getClaimAsString("preferred_username"));

        // Nếu email chưa được verify
        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        return userRepository.findByKeycloakId(keycloakId)
                .map(existingUser ->
                        syncUserFromToken(existingUser, jwt, username, email)
                )
                // Account Linking tự động theo Email
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email)
                        .map(existingUser -> {
                            User linkedUser = linkExistingUser(existingUser, keycloakId, username);
                            return syncUserFromToken(linkedUser, jwt, username, email);
                        })
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
    }

    @Override
    @Transactional
    public UserDetailsResponse removeRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String keycloakId = user.getKeycloakId();

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

        // Xoá ở keycloak
        keycloakAdminService.removeClientRole(
                keycloakId,
                actualRoleName
        );

        user.getUserHasRoles().remove(assignedRole);

        User updatedUser = userRepository.saveAndFlush(user);
        return userMapper.toUserDetailResponse(updatedUser);

    }

    @Override
    public void logoutUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean targetIsAdmin = user.getUserHasRoles().stream()
                .anyMatch(userRole ->
                        "ADMIN".equalsIgnoreCase(
                                userRole.getRole().getRoleName()
                        )
                );
        if (targetIsAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        keycloakAdminService.logoutUser(
                requireKeycloakId(user)
        );
    }

    @Override
    public List<UserSessionResponse> getUserSessions(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return keycloakAdminService.getUserSessions(
                requireKeycloakId(user)
        );
    }

    @Override
    public List<UserCredentialResponse> getUserCredentials(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return keycloakAdminService.getUserCredentials(
                requireKeycloakId(user)
        );
    }

    @Override
    public void deleteUserCredential(String userId, String credentialId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String keycloakId = requireKeycloakId(user);
        UserCredentialResponse credential = keycloakAdminService
                .getUserCredentials(keycloakId)
                .stream()
                .filter(item -> credentialId.equals(item.credentialId()))
                .findFirst()
                .orElseThrow(() ->
                        new AppException(ErrorCode.USER_CREDENTIAL_NOT_FOUND)
                );

        if (credential.type() == null
                || !DELETABLE_CREDENTIAL_TYPES.contains(
                        credential.type().toLowerCase(Locale.ROOT)
                )) {
            throw new AppException(
                    ErrorCode.USER_CREDENTIAL_DELETE_NOT_ALLOWED
            );
        }

        keycloakAdminService.deleteUserCredential(keycloakId, credentialId);
    }

    @Override
    public void terminateUserSession(String userId, String sessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean targetIsAdmin = user.getUserHasRoles().stream()
                .anyMatch(userRole ->
                        "ADMIN".equalsIgnoreCase(
                                userRole.getRole().getRoleName()
                        )
                );
        if (targetIsAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        String keycloakId = requireKeycloakId(user);
        boolean sessionBelongsToUser = keycloakAdminService.getUserSessions(keycloakId)
                .stream()
                .anyMatch(session -> sessionId.equals(session.sessionId()));

        if (!sessionBelongsToUser) {
            throw new AppException(ErrorCode.USER_SESSION_NOT_FOUND);
        }

        keycloakAdminService.deleteUserSession(sessionId);
    }

    @Override
    public void sendRequiredActionsEmail(
            String userId,
            SendRequiredActionsEmailRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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

    private User syncUserFromToken(User user, Jwt jwt, String username, String email) {
        user.setEmail(email);
        user.setUsername(username);
        user.setFirstName(requiredClaim(jwt.getClaimAsString("given_name")));
        user.setLastName(requiredClaim(jwt.getClaimAsString("family_name")));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private void validateUsernameAvailable(String username) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    private String requireKeycloakId(User user) {
        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            throw new AppException(ErrorCode.KEYCLOAK_USER_NOT_LINKED);
        }
        return user.getKeycloakId();
    }

    private String requiredClaim(String claims){
        if (claims == null || claims.isBlank()){
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return claims.strip();
    }
}
