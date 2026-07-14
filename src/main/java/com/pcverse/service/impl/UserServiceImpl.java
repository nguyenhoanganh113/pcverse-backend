package com.pcverse.service.impl;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.entity.Role;
import com.pcverse.entity.User;
import com.pcverse.enums.Gender;
import com.pcverse.enums.UserStatus;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.mapper.UserMapper;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.RoleService;
import com.pcverse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleService roleService;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {

        // 1. Convert DTO sang Entity
        User user = userMapper.toUser(request);

        // 2. Mã hoá password
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserStatus(UserStatus.ACTIVE);

        // 3. Tạo hoặc lấy role CUSTOMER
        Role role = roleService.createRole("CUSTOMER");

        // 4. Gán role cho user
        user.addRole(role);

        // 5. Lưu user vào database
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            log.error("User already exists");
            throw new UserServiceException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 6. Convert Entity sang Response DTO
        return userMapper.toCreateUserResponse(user);
    }

    @Override
    public UserDetailsResponse myInfo(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserServiceException(ErrorCode.USER_NOT_FOUND));

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
    public User ensureUserExistsFromToken(Jwt jwt) {
        String keycloakId = requireClaim(jwt.getSubject());
        String email = requireClaim(jwt.getClaimAsString("email"))
                .toLowerCase(Locale.ROOT);
        String username = firstNonBlank(jwt.getClaimAsString("preferred_username"), email);

        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        }

        return userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email)
                        .map(existingUser -> linkExistingUser(existingUser, keycloakId, username))
                        .orElseGet(() -> createUserFromToken(jwt, keycloakId, username, email)));
    }

    private User linkExistingUser(User user, String keycloakId, String username) {
        if (user.getKeycloakId() != null && !user.getKeycloakId().equals(keycloakId)) {
            throw new UserServiceException(ErrorCode.USER_ALREADY_EXISTS);
        }

        user.setKeycloakId(keycloakId);
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(username);
        }
        return userRepository.save(user);
    }

    private User createUserFromToken(Jwt jwt, String keycloakId, String username, String email) {
        String firstName = firstNonBlank(
                jwt.getClaimAsString("given_name"),
                username,
                usernameFromEmail(email)
        );

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                // Password authentication is managed by Keycloak; this satisfies the legacy NOT NULL column.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(firstName)
                .lastName(firstNonBlank(jwt.getClaimAsString("family_name"), ""))
                .phoneNumber(trimToNull(jwt.getClaimAsString("phone_number")))
                .urlAvatar(jwt.getClaimAsString("picture"))
                .gender(toGender(jwt.getClaimAsString("gender")))
                .userStatus(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private String requireClaim(String value) {
        if (value == null || value.isBlank()) {
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String usernameFromEmail(String email) {
        int separatorIndex = email.indexOf('@');
        return separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
    }

    private Gender toGender(String value) {
        if (value == null || value.isBlank()) {
            return Gender.OTHER;
        }

        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Gender.OTHER;
        }
    }

}
