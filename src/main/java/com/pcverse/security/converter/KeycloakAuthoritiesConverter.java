package com.pcverse.security.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class KeycloakAuthoritiesConverter implements AuthoritiesConverter {

    @Value("${keycloak.resource-client-id:pc-verse-backend}")
    String resourceClientId;

    @Override
    public Collection<GrantedAuthority> convert(Map<String, Object> claims) {

        if (!(claims.get("resource_access")
                instanceof Map<?, ?> resourceAccess)) {
            return List.of();
        }

        if (!(resourceAccess.get(resourceClientId)
                instanceof Map<?, ?> clientAccess)) {
            return List.of();
        }

        if (!(clientAccess.get("roles")
                instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
