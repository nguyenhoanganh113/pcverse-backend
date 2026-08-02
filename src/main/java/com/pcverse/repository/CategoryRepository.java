package com.pcverse.repository;

import com.pcverse.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findAllByActive(boolean active);

    Optional<Category> findByIdAndActiveTrue(String id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    boolean existsBySlugAndIdNot(String slug, String id);

}
