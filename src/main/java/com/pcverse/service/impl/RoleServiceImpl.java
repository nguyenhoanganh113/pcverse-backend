package com.pcverse.service.impl;

import com.pcverse.entity.Role;
import com.pcverse.enums.RealmRole;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.repository.RoleRepository;
import com.pcverse.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role getRoleByName(String roleName) {
        String normalizedRoleName = normalizeAndValidate(roleName);

        return roleRepository.findByRoleName(normalizedRoleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private String normalizeAndValidate(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        String normalizedRoleName =
                roleName.strip().toUpperCase(Locale.ROOT);

        try {
            RealmRole.valueOf(normalizedRoleName);
            return normalizedRoleName;
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
    }
}
