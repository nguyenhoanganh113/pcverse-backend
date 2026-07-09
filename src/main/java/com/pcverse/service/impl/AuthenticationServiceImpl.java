package com.pcverse.service.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.LoginResponse;
import com.pcverse.entity.SecurityUser;
import com.pcverse.entity.User;
import com.pcverse.enums.TokenType;
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

import java.text.ParseException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pcverse.constant.AppConstant.TOKEN_TYPE;

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

        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

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
    public LoginResponse refreshToken(String refreshToken) {
        try {
            SignedJWT signedJWT = jwtService.validateToken(refreshToken);
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            String tokenType = signedJWT.getJWTClaimsSet().getStringClaim(TOKEN_TYPE);

            if (!TokenType.REFRESH_TOKEN.name().equals(tokenType)) {
                throw new UserServiceException(ErrorCode.TOKEN_INVALID);
            }

            User user = userRepository.findWithAuthoritiesById(userId)
                    .orElseThrow(() -> new UserServiceException(ErrorCode.USER_NOT_FOUND));

            Set<String> roles = extractAuthorities(new SecurityUser(user));

            String newAccessToken = jwtService.generateAccessToken(userId, roles);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .roles(roles)
                    .build();

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
