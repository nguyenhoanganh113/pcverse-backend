package com.pcverse.repository.specification;

import com.pcverse.entity.Category;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

// Utility class
@UtilityClass
public class CategorySpecification {

    public Specification<Category> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {

            if(!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = keyword
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String escapedKeyword =
                    escapeLikePattern(normalizedKeyword);

            String pattern = "%" + escapedKeyword + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("slug")), pattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern, '\\')
            );

        };
    }

    public Specification<Category> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if(active == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

}
