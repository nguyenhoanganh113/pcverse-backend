package com.pcverse.repository;

import com.pcverse.entity.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, String>,
        JpaSpecificationExecutor<AttributeOption> {

    @Query("""
            select (count(attributeOption) > 0)
            from AttributeOption attributeOption
            where attributeOption.attributeDefinition.id = :attributeDefinitionId
              and lower(attributeOption.code) in :codes
            """)
    boolean existsAnyByAttributeDefinitionIdAndCodes(
            @Param("attributeDefinitionId") String attributeDefinitionId,
            @Param("codes") Collection<String> codes
    );

    Optional<AttributeOption> findByIdAndAttributeDefinitionId(String attributeOptionId, String attributeDefinitionId);

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCaseAndIdNot(String attributeDefinitionId, String code, String attributeOptionId);

}
