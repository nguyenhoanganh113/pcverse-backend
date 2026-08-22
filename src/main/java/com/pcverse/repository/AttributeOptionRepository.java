package com.pcverse.repository;

import com.pcverse.entity.AttributeOption;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@NullMarked
@Repository
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, String>,
        JpaSpecificationExecutor<AttributeOption> {

    @Override
    @EntityGraph(attributePaths = "attributeDefinition")
    Page<AttributeOption> findAll(
            Specification<AttributeOption> specification,
            Pageable pageable
    );

    boolean existsByAttributeDefinition_IdAndCodeIn(String attributeDefinitionId, Collection<String> codes);

    Optional<AttributeOption> findByIdAndAttributeDefinition_Id(String attributeOptionId, String attributeDefinitionId);

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCaseAndIdNot(String attributeDefinitionId, String code, String attributeOptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ao
        from AttributeOption ao
        where ao.id = :attributeOptionId
          and ao.attributeDefinition.id = :attributeDefinitionId
        """)
    Optional<AttributeOption> findForUpdate(
            @Param("attributeOptionId") String attributeOptionId,
            @Param("attributeDefinitionId") String attributeDefinitionId
    );

}
