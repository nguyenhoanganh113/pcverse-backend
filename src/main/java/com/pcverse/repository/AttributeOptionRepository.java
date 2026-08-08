package com.pcverse.repository;

import com.pcverse.entity.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, String>,
        JpaSpecificationExecutor<AttributeOption> {

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCase(String attributeDefinitionId, String code);

    Optional<AttributeOption> findByIdAndAttributeDefinitionId(String attributeOptionId, String attributeDefinitionId);

    boolean existsByAttributeDefinitionIdAndCodeIgnoreCaseAndIdNot(String attributeDefinitionId, String code, String attributeOptionId);

}
