package com.pcverse.repository;

import com.pcverse.entity.CategoryAttribute;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAttributeRepository extends
        JpaRepository<CategoryAttribute, String>,
        JpaSpecificationExecutor<CategoryAttribute> {

    boolean existsByCategory_IdAndAttributeDefinition_Id(String categoryId, String attributeId);

    boolean existsByAttributeDefinition_Id(String attributeId);

    boolean existsByCategory_Id(String categoryId);

    @EntityGraph(
            attributePaths = {
                    "category",
                    "attributeDefinition"
            }
    )
    Optional<CategoryAttribute> findByIdAndCategory_Id(String categoryAttributeId, String categoryId);

    @EntityGraph(
            attributePaths = {
                    "category",
                    "attributeDefinition"
            }
    )
    List<CategoryAttribute> findAllByCategory_Id(String categoryId);

    @EntityGraph(
            attributePaths = {
                    "attributeDefinition",
                    "attributeDefinition.attributeOptions"
            }
    )
    List<CategoryAttribute>
    findAllByCategory_IdAndFilterableTrueAndAttributeDefinition_ActiveTrueOrderByDisplayOrderAsc(
            String categoryId
    );

    @Override
    @EntityGraph(
            attributePaths = {
                    "category",
                    "attributeDefinition"
            }
    )
    Page<CategoryAttribute> findAll(
            @NonNull Specification<CategoryAttribute> specification,
            @NonNull Pageable pageable
    );

    boolean existsByAttributeDefinition_IdAndCategory_ActiveTrue(String attributeDefinitionId);

    @Query("""
            select categoryAttribute.attributeDefinition.id
            from CategoryAttribute categoryAttribute
            where categoryAttribute.category.id = :categoryId
            order by categoryAttribute.attributeDefinition.id
            """)
    List<String> findAttributeDefinitionIdsByCategoryId(
            @Param("categoryId") String categoryId
    );

}
