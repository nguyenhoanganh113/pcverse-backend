package com.pcverse.configuration;

import com.pcverse.entity.Role;
import com.pcverse.enums.RealmRole;
import com.pcverse.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RealmRole realmRole : RealmRole.values()) {
            createIfMissing(realmRole);
        }
    }

    private void createIfMissing(RealmRole realmRole) {
        roleRepository.findByRoleName(realmRole.name())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .roleName(realmRole.name())
                                .description(realmRole.description())
                                .build()
                ));
    }
}
