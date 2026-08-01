package com.pcverse.repository;

import com.pcverse.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends
        JpaRepository<User, String>,
        JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByKeycloakId(String keycloakId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT user FROM User user WHERE user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") String userId);

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

    @EntityGraph(attributePaths = {
            "userHasRoles",
            "userHasRoles.role"
    })
    @Query("""
        SELECT user
        FROM User user
        WHERE user.id = :userId
        """)
    Optional<User> findDetailsById(@Param("userId") String userId);

}
