package com.pcverse.entity;

import com.pcverse.enums.UserStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SecurityUser(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Stream<String> roles = user.getUserHasRoles().stream()
                .map(UserHasRole::getRole)
                .filter(role -> role != null && role.getRoleName() != null)
                .map(role -> "ROLE_" + role.getRoleName());

        Stream<String> permissions = user.getUserHasRoles().stream()
                .map(UserHasRole::getRole)
                .filter(role -> role != null && role.getRolePermissions() != null)
                .flatMap(role -> role.getRolePermissions().stream())
                .map(RoleHasPermission::getPermission)
                .filter(permission -> permission != null && permission.getName() != null)
                .map(Permission::getName);

        return Stream.concat(roles, permissions)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getUserStatus() == UserStatus.ACTIVE;
    }
}
