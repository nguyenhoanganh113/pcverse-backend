package com.pcverse.repository;

import com.pcverse.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {
            "userHasRoles",
            "userHasRoles.role",
            "userHasRoles.role.rolePermissions",
            "userHasRoles.role.rolePermissions.permission"
    })
    Optional<User> findWithAuthoritiesById(String id);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT DISTINCT user
            FROM User user
            LEFT JOIN FETCH user.userHasRoles userHasRole
            LEFT JOIN FETCH userHasRole.role
            WHERE user.id IN :userIds
            """)
    List<User> findAllWithRolesByIdIn(
            @Param("userIds") Collection<String> userIds
    );

}
