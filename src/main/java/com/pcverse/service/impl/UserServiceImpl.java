package com.pcverse.service.impl;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.UserMapper;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.RoleService;
import com.pcverse.service.KeycloakAdminService;
import com.pcverse.service.UserService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${keycloak.admin.realm}")
    private String realm;

    private final UserRepository userRepository;
    private final Keycloak keycloak;
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final KeycloakAdminService keycloakAdminService;

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsernameIgnoreCase(request.username()) ||
                userRepository.existsByEmailIgnoreCase(request.email())) {

            log.error("Username or Email already exists when admin create a user");
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);

        }

        // 1. Convert DTO sang Entity (bỏ qua password)
        User user = userMapper.toUser(request);
        user.setUserStatus(UserStatus.ACTIVE);

        try {
            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setUsername(request.username());
            userRepresentation.setEmail(request.email());
            userRepresentation.setFirstName(request.firstName());
            userRepresentation.setLastName(request.lastName());
            userRepresentation.setEnabled(true);
            userRepresentation.setEmailVerified(true);

            CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            credentialRepresentation.setValue(request.password());
            credentialRepresentation.setTemporary(false);
            userRepresentation.setCredentials(List.of(credentialRepresentation));

            Response response = keycloak.realm(realm).users().create(userRepresentation);

            try (response) {
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    log.error("Keycloak returned status {} while creating user", response.getStatus());
                    throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
                }
            }

            String userId = CreatedResponseUtil.getCreatedId(response);
            user.setKeycloakId(userId);
            userRepository.save(user);
            log.info("User created with id {}", userId);
        } catch (Exception e) {
            log.error("Error creating user profile: {}", e.getMessage());
            userRepository.delete(user);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }

        // 6. Convert Entity sang Response DTO
        return userMapper.toCreateUserResponse(user);
    }

    @Override
    @Transactional
    public UserDetailsResponse myInfo(Jwt jwt) {

        User user = ensureUserExistsFromToken(jwt);

        return userMapper.toUserDetailResponse(user);
    }

    @Override
    // @PreAuthorize("hasRole('ADMIN')") cái này sẽ đi được vào service nên cần phải tạo 1 hàm để catch được
    // khi throw AuthorizationDeniedException vì exception này thuộc service layer nên không xử lý ở nhánh này
    public List<UserDetailsResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserDetailResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserDetailsResponse updateUserStatus(String userId, UserStatus status) {
        User user = findUser(userId);
        String keycloakId = requireKeycloakId(user);

        boolean enabled = switch (status) {
            case ACTIVE -> true;
            case DISABLED -> false;
            case LOCKED, PENDING_VERIFICATION ->
                    throw new AppException(ErrorCode.USER_STATUS_NOT_SUPPORTED);
        };

        // Keycloak is updated first so a failed remote call does not leave local DB disabled
        // while the user can still authenticate through Keycloak.
        keycloakAdminService.setUserEnabled(keycloakId, enabled);

        user.setUserStatus(status);
        return userMapper.toUserDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailsResponse updateUser(String userId, UpdateAdminUserRequest request) {
        User user = findUser(userId);
        String keycloakId = requireKeycloakId(user);

        validateEmailAvailable(user, request.email());
        keycloakAdminService.updateUser(keycloakId, request);

        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        user.setGender(request.gender());
        user.setDateOfBirth(request.dateOfBirth());
        user.setUrlAvatar(request.urlAvatar());

        try {
            return userMapper.toUserDetailResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = findUser(userId);
        keycloakAdminService.deleteUser(requireKeycloakId(user));
        userRepository.delete(user);
        userRepository.flush();
    }

    @Override
    public void resetPassword(String userId, ResetUserPasswordRequest request) {
        User user = findUser(userId);
        keycloakAdminService.resetPassword(
                requireKeycloakId(user),
                request.newPassword(),
                request.isTemporary()
        );
    }

    @Override
    @Transactional
    public UserDetailsResponse assignRole(String userId, String roleName) {
        User user = findUser(userId);

        boolean alreadyAssigned = user.getUserHasRoles().stream()
                .anyMatch(userRole -> roleName.equalsIgnoreCase(userRole.getRole().getRoleName()));
        if (alreadyAssigned) {
            return userMapper.toUserDetailResponse(user);
        }

        keycloakAdminService.assignClientRole(requireKeycloakId(user), roleName);
        user.addRole(roleService.createRole(roleName));
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

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String requireKeycloakId(User user) {
        if (user.getKeycloakId() == null || user.getKeycloakId().isBlank()) {
            throw new AppException(ErrorCode.KEYCLOAK_USER_NOT_LINKED);
        }
        return user.getKeycloakId();
    }

    private void validateEmailAvailable(User currentUser, String email) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .ifPresent(user -> {
                    throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
                });
    }

    private String requiredClaim(String claims){
        if (claims == null || claims.isBlank()){
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return claims.strip();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    private String usernameFromEmail(String email) {
        int separatorIndex = email.indexOf('@');
        return separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
    }

}
