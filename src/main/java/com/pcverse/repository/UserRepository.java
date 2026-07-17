package com.pcverse.repository;

import com.pcverse.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {
            "userHasRoles",
            "userHasRoles.role",
            "userHasRoles.role.rolePermissions",
            "userHasRoles.role.rolePermissions.permission"
    })
    Optional<User> findWithAuthoritiesById(String id);

    Optional<User> findByKeycloakId(String keycloakId);

    Boolean existsByUsernameIgnoreCase(String username);

}
