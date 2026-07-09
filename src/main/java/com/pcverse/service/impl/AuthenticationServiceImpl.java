package com.pcverse.service.impl;

import com.nimbusds.jose.JOSEException;
import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.ExchangeTokenResponse;
import com.pcverse.dto.response.LoginResponse;
import com.pcverse.dto.response.TokenPayloadResponse;
import com.pcverse.entity.SecurityUser;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.AuthenticationService;
import com.pcverse.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        Object principal = authenticate.getPrincipal();
        if (!(principal instanceof SecurityUser securityUser)) {
            throw new UserServiceException(ErrorCode.UNAUTHORIZED);
        }

        User user = securityUser.getUser();
        Set<String> roles = extractAuthorities(securityUser);

        // Generate access token và refresh token dựa trên userId và authorities
        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(roles)
                .build();

    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeTokenResponse refreshToken(String refreshToken) {
        try {
            TokenPayloadResponse payload = jwtService.verifyRefreshToken(refreshToken);
            String userId = payload.id();

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
    public void logout(String accessToken, String refreshToken) {

    }

    private Set<String> extractAuthorities(SecurityUser securityUser) {
        return securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
