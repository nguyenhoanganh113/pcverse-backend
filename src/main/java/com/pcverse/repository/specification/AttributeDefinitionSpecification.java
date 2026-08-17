package com.pcverse.repository.specification;

import com.pcverse.entity.AttributeDefinition;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class AttributeDefinitionSpecification {

    public Specification<AttributeDefinition> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = keyword.strip().toLowerCase(Locale.ROOT);
            String escapedKeyword = escapeLikePattern(normalizedKeyword);
            String pattern = "%" + escapedKeyword + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern, '\\')
            );
        };
    }

    public Specification<AttributeDefinition> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if(active == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

}
