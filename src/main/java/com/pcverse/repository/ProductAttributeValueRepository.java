package com.pcverse.repository;

import com.pcverse.entity.ProductAttributeValue;
import com.pcverse.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Query("""
            select productAttributeValue.attributeDefinition.id
            from ProductAttributeValue productAttributeValue
            where productAttributeValue.product.id = :productId
            order by productAttributeValue.attributeDefinition.id
            """)
    List<String> findAttributeDefinitionIdsByProductId(
            @Param("productId") String productId
    );

    @Query("""
            select productAttributeValue.attributeOption.id
            from ProductAttributeValue productAttributeValue
            where productAttributeValue.product.id = :productId
            order by productAttributeValue.attributeOption.id
            """)
    List<String> findAttributeOptionIdsByProductId(
            @Param("productId") String productId
    );

}
