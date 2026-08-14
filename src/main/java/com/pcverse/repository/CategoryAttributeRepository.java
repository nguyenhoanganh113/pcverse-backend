package com.pcverse.repository;

import com.pcverse.entity.CategoryAttribute;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, String> {

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

}
