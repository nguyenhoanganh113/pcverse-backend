package com.pcverse.repository;

import com.pcverse.entity.AttributeOption;
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
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, String>,
        JpaSpecificationExecutor<AttributeOption> {

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCase(String attributeDefinitionId, String code);

    Optional<AttributeOption> findByIdAndAttributeDefinitionId(String attributeOptionId, String attributeDefinitionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attributeOption
            from AttributeOption attributeOption
            where attributeOption.id = :attributeOptionId
              and attributeOption.attributeDefinition.id = :attributeDefinitionId
            """)
    Optional<AttributeOption> findByIdAndAttributeDefinitionIdForUpdate(
            @Param("attributeOptionId") String attributeOptionId,
            @Param("attributeDefinitionId") String attributeDefinitionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attributeOption
            from AttributeOption attributeOption
            where attributeOption.id in :ids
            order by attributeOption.id
            """)
    List<AttributeOption> findAllByIdForUpdate(
            @Param("ids") Collection<String> ids
    );

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCaseAndIdNot(String attributeDefinitionId, String code, String attributeOptionId);

}
