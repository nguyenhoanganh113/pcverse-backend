package com.pcverse.repository.specification;

import com.pcverse.entity.CategoryAttribute;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class CategoryAttributeSpecification {

    public Specification<CategoryAttribute> belongsToCategory(
            String categoryId
    ) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("category").get("id"),
                categoryId
        );
    }

    public Specification<CategoryAttribute> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            String escapedKeyword = escapeLikePattern(normalizedKeyword);
            String pattern = "%" + escapedKeyword + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("attributeDefinition").get("code")
                            ),
                            pattern,
                            '\\'
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("attributeDefinition").get("name")
                            ),
                            pattern,
                            '\\'
                    )
            );
        };
    }

    public Specification<CategoryAttribute> hasRequired(Boolean required) {
        return hasBooleanField("required", required);
    }

    public Specification<CategoryAttribute> hasFilterable(Boolean filterable) {
        return hasBooleanField("filterable", filterable);
    }

    public Specification<CategoryAttribute> hasHighlighted(Boolean highlighted) {
        return hasBooleanField("highlighted", highlighted);
    }

    private Specification<CategoryAttribute> hasBooleanField(
            String field,
            Boolean value
    ) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get(field), value);
        };
    }
}
