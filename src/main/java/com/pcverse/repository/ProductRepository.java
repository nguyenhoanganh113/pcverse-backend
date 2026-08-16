package com.pcverse.repository;

import com.pcverse.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, String id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    boolean existsByCategory_Id(String categoryId);

    boolean existsByBrand_Id(String brandId);

    boolean existsByCategory_IdAndProductStatus(
            String categoryId,
            com.pcverse.enums.ProductStatus productStatus
    );

    boolean existsByBrand_IdAndProductStatus(
            String brandId,
            com.pcverse.enums.ProductStatus productStatus
    );

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findById(String id);

    @EntityGraph(attributePaths = {
            "category",
            "brand",
            "attributeValues",
            "attributeValues.attributeDefinition",
            "attributeValues.attributeOption"
    })
    @Query("select product from Product product where product.id = :productId")
    Optional<Product> findByIdWithAttributeValues(
            @Param("productId") String productId
    );

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Product product
            set product.version = product.version + 1
            where product.id = :productId
              and product.version = :expectedVersion
            """)
    int incrementVersionIfMatches(
            @Param("productId") String productId,
            @Param("expectedVersion") Long expectedVersion
    );
}
