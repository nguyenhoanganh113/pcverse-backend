package com.pcverse.repository;

import com.pcverse.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, String>, JpaSpecificationExecutor<AttributeDefinition> {

    boolean existsByCode(String code);

}
