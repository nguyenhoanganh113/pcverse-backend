package com.pcverse.configuration;

import com.pcverse.service.RedisTokenService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    private NimbusJwtDecoder nimbusJwtDecoder;

    // Inject RedisTokenService để check blacklist
    private final RedisTokenService redisTokenService;
    private final JwtKeyProvider jwtKeyProvider;

    // private final TokenRepository tokenRepository;

    @PostConstruct
    public void init() {
        // Dùng public key để verify chữ ký access token được ký bằng private key RS256.
        nimbusJwtDecoder = NimbusJwtDecoder
                .withPublicKey(jwtKeyProvider.getAccessTokenPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Override
    public Jwt decode(@NonNull String token) throws JwtException {

        Jwt jwt = nimbusJwtDecoder.decode(token);
        String jwtId = jwt.getId();
        if (jwtId == null) {
            throw new BadJwtException("JWT ID is missing");
        }
        if (redisTokenService.existsByJwtId(jwtId)) {
            throw new BadJwtException("Token has been revoked");
        }
        return jwt;
    }
}
