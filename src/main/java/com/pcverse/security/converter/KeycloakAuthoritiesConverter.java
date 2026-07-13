package com.pcverse.security.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class KeycloakAuthoritiesConverter implements AuthoritiesConverter {

    @Value("${keycloak.resource-client-id:pc-verse-api}")
    String resourceClientId;

    @Override
    public Collection<GrantedAuthority> convert(Map<String, Object> source) {

        /*
         * access claim dự kiến có cấu trúc:
         *
         * {
         *     "roles": ["ADMIN", "CUSTOMER"]
         * }
         *
         * Function này chuyển access claim từ Object
         * thành Stream<String> chứa các role.
         */
        Function<Object, Stream<String>> rolesFromAccessClaim = accessClaim -> {
            if (!(accessClaim instanceof Map<?, ?> access)
                    || !(access.get("roles") instanceof Collection<?> roles)) {
                return Stream.empty();
            }

            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast);
        };

        // Lấy role áp dụng trên toàn realm.
        Stream<String> realmRoles =
                rolesFromAccessClaim.apply(source.get("realm_access"));

        Stream<String> clientRoles = Stream.empty();

        /*
         * resource_access có cấu trúc:
         *
         * {
         *     "pc-verse-api": {
         *         "roles": ["ADMIN", "CUSTOMER"]
         *     }
         * }
         */
        if (source.get("resource_access")
                instanceof Map<?, ?> resourceAccess) {

            Object clientAccess =
                    resourceAccess.get(resourceClientId);

            clientRoles =
                    rolesFromAccessClaim.apply(clientAccess);
        }

        return Stream.concat(realmRoles, clientRoles)
                // Xóa khoảng trắng đầu và cuối.
                .map(String::trim)

                // Loại bỏ role rỗng.
                .filter(role -> !role.isBlank())

                // Chuẩn hóa thành chữ hoa.
                .map(role -> role.toUpperCase(Locale.ROOT))

                // hasRole("ADMIN") yêu cầu authority ROLE_ADMIN.
                .map(role -> role.startsWith("ROLE_")
                        ? role
                        : "ROLE_" + role)

                // Loại bỏ role trùng giữa realm và client.
                .distinct()

                // Chuyển String thành GrantedAuthority.
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
