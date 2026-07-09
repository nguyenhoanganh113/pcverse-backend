package com.pcverse.service.impl;

import com.nimbusds.jose.JOSEException;
import com.pcverse.dto.TokenDetails;
import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.ExchangeTokenResponse;
import com.pcverse.dto.response.LoginResponse;
import com.pcverse.dto.response.TokenPayloadResponse;
import com.pcverse.entity.RedisToken;
import com.pcverse.entity.SecurityUser;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.AuthenticationService;
import com.pcverse.service.JwtService;
import com.pcverse.service.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RedisTokenService redisTokenService;

    @Override
    public LoginResponse login(LoginRequest request) {

        // 1. Xác thực user
        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        // 2. Lấy thông tin user
        Object principal = authenticate.getPrincipal();
        if (!(principal instanceof SecurityUser securityUser)) {
            throw new UserServiceException(ErrorCode.UNAUTHORIZED);
        }

        // 3. Extract roles
        User user = securityUser.getUser();
        Set<String> roles = extractAuthorities(securityUser);

        // Generate access token và refresh token dựa trên userId và authorities
        // 4. Generate access token (String)
        String accessToken = jwtService.generateAccessToken(user.getId(), roles);

        // 5. Generate refresh token (TokenDetails)
        TokenDetails tokenDetails = jwtService.generateRefreshToken(user.getId());

        // 6. Lưu refresh token vào Redis
        RedisToken redisToken = RedisToken.builder()
                .jwtId(tokenDetails.jwtId())      // UUID của token
                .userId(user.getId())              // User ID để query sau này
                .expiration(tokenDetails.ttlSeconds()) // TTL = 7 ngày
                .build();

        redisTokenService.saveToken(redisToken);

        // 7. Return response
        // Chỉ return token string (value), không return jwtId và ttl
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(tokenDetails.value())
                .roles(roles)
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeTokenResponse refreshToken(String refreshToken) {
        try {
            TokenPayloadResponse payload = jwtService.verifyRefreshToken(refreshToken);
            String userId = payload.userId();

            User user = userRepository.findWithAuthoritiesById(userId)
                    .orElseThrow(() -> new UserServiceException(ErrorCode.TOKEN_INVALID));

            if (user.getUserStatus() != UserStatus.ACTIVE) {
                throw new UserServiceException(ErrorCode.UNAUTHORIZED);
            }

            Set<String> roles = extractAuthorities(new SecurityUser(user));
            String newAccessToken = jwtService.generateAccessToken(userId, roles);

            return ExchangeTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .roles(roles)
                    .build();

        } catch (ParseException | JOSEException e) {
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    public void logout(String refreshToken) {

        // 1. Validate refresh token có tồn tại không
        if (refreshToken == null) {
            throw new UserServiceException(ErrorCode.MISSING_LOGOUT_INFO);
        }

        // 2. Lấy thông tin user từ SecurityContext (từ access token)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null)
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        String userId = authentication.getName();

        // 3. Lấy thông tin access token từ SecurityContext
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        }

        try {
            // 4. Validate refresh token và lấy thông tin trong payload
            TokenPayloadResponse refreshPayload = jwtService.verifyRefreshToken(refreshToken);

            // 5. Verify userId từ access token và refresh token phải giống nhau
            // Tránh trường hợp user A dùng access token của mình + refresh token của user B
            String refreshUserId = refreshPayload.userId();
            if (!Objects.equals(userId, refreshUserId)) {
                throw new UserServiceException(ErrorCode.TOKEN_INVALID);
            }

            // 6. Xóa refresh token khỏi Redis
            // Refresh token đã được lưu vào Redis khi login
            String refreshJwtId = refreshPayload.jwtId();
            if (refreshJwtId == null || !redisTokenService.existsByJwtId(refreshJwtId)) {
                throw new UserServiceException(ErrorCode.TOKEN_INVALID);
            }
            redisTokenService.deleteTokenByJwtId(refreshJwtId);

            String accessJwtId = jwt.getId();
            Instant now = Instant.now();
            Instant accessExpiration = jwt.getExpiresAt();

            // 7. Tính TTL còn lại của access token
            // TTL = thời gian hết hạn - thời gian hiện tại
            long ttl = accessExpiration != null
                    ? ChronoUnit.SECONDS.between(now, accessExpiration)
                    : 0;

            // 8. Nếu access token còn hạn → lưu vào Redis blacklist
            // Nếu đã hết hạn (ttl <= 0) → không cần lưu vì token đã invalid
            if (ttl > 0) {
                redisTokenService.saveToken(
                        RedisToken.builder()
                                .jwtId(accessJwtId)
                                .userId(userId)
                                .expiration(ttl)
                                .build()
                );
            }

        } catch (ParseException | JOSEException e) {
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);
        }

    }

    private Set<String> extractAuthorities(SecurityUser securityUser) {
        return securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
