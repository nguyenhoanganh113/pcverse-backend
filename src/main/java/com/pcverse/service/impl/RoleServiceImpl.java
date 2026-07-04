package com.pcverse.service.impl;

import com.pcverse.entity.Role;
import com.pcverse.repository.RoleRepository;
import com.pcverse.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role createRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName(roleName)
                                .build()
                ));
    }
}
