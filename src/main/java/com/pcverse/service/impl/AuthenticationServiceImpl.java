package com.pcverse.service.impl;

import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.LoginResponse;
import com.pcverse.entity.SecurityUser;
import com.pcverse.entity.User;
import com.pcverse.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationManager authenticationManager;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);

        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();

        User user = securityUser.user();

        Set<String> authorities = extractAuthorities(securityUser);

        // Generate access token và refresh token dựa trên userId và authorities
        /**
        String accessToken = jwtService.generateAccessToken(user.getId(), authorities);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), authorities);

         */

        return LoginResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .roles(authorities)
                .build();

    }

    private Set<String> extractAuthorities(SecurityUser securityUser) {
        return securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
