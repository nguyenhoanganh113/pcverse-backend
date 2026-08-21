package com.pcverse.repository;

import com.pcverse.entity.AttributeDefinition;
import jakarta.persistence.LockModeType;
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
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, String>, JpaSpecificationExecutor<AttributeDefinition> {

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attributeDefinition
            from AttributeDefinition attributeDefinition
            where attributeDefinition.id = :id
            """)
    Optional<AttributeDefinition> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attributeDefinition
            from AttributeDefinition attributeDefinition
            where attributeDefinition.id in :ids
            order by attributeDefinition.id
            """)
    List<AttributeDefinition> findAllByIdForUpdate(
            @Param("ids") Collection<String> ids
    );

}
