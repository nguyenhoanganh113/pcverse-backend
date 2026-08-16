package com.pcverse.repository;

import com.pcverse.entity.ProductAttributeValue;
import com.pcverse.enums.ProductStatus;
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

    boolean existsByAttributeOption_IdAndProduct_ProductStatus(String attributeOptionId, ProductStatus productStatus);

    boolean existsByAttributeDefinition_IdAndProduct_ProductStatus(String attributeDefinitionId, ProductStatus productStatus);

}
