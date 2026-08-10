package com.pcverse.repository;

import com.pcverse.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, String> {

    boolean existsByAttributeDefinition_Id(String id);

    boolean existsByAttributeOption_Id(String id);

    boolean existsByProduct_Category_IdAndAttributeDefinition_Id(
            String categoryId,
            String attributeDefinitionId
    );

}
