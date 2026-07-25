package com.pcverse.configuration;

import com.pcverse.exception.TokenRevokedException;
import com.pcverse.service.RedisTokenService;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
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
    private final String resourceClientId;

    public CustomJwtDecoder(
            RedisTokenService redisTokenService,
            @Value("${keycloak.issuer-uri}") String issuerUri,
            @Value("${keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${keycloak.resource-client-id}") String resourceClientId) {
        this.redisTokenService = redisTokenService;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.resourceClientId = resourceClientId;
    }

    @PostConstruct
    public void init() {
        // Chọn public key theo kid từ JWKS của Keycloak và cache key để verify token.
        nimbusJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Validate JWT theo mặc định của Spring Security:
        // kiểm tra loại token, thời gian hiệu lực (exp/nbf) và issuer của realm Keycloak.
        OAuth2TokenValidator<Jwt> defaultValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        // Chỉ chấp nhận token được phát cho resource server đã cấu hình
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtAudienceValidator(resourceClientId);

        nimbusJwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));
    }

    @Override
    public Jwt decode(@NonNull String token) throws JwtException {

        Jwt jwt = nimbusJwtDecoder.decode(token);
        String jwtId = jwt.getId();
        if (jwtId == null || jwtId.isBlank()) {
            throw new BadJwtException("JWT ID is missing");
        }
        if (redisTokenService.existsByJwtId(jwtId)) {
            throw new TokenRevokedException();
        }

        String keycloakId = jwt.getSubject();
        if (keycloakId != null && redisTokenService.isUserTokenRevoked(keycloakId, jwt.getIssuedAt())) {
            throw new TokenRevokedException();
        }

        return jwt;
    }
}
