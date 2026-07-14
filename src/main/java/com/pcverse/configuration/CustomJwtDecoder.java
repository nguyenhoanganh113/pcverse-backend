package com.pcverse.configuration;

import com.pcverse.service.RedisTokenService;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    private NimbusJwtDecoder nimbusJwtDecoder;

    // Inject RedisTokenService để check blacklist
    private final RedisTokenService redisTokenService;
    private final String issuerUri;
    private final String jwkSetUri;

    public CustomJwtDecoder(
            RedisTokenService redisTokenService,
            @Value("${keycloak.issuer-uri}") String issuerUri,
            @Value("${keycloak.jwk-set-uri}") String jwkSetUri) {
        this.redisTokenService = redisTokenService;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
    }

    @PostConstruct
    public void init() {
        // Chọn public key theo kid từ JWKS của Keycloak và cache key để verify token.
        nimbusJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        nimbusJwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri));
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
