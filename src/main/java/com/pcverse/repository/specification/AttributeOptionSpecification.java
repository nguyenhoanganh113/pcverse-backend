package com.pcverse.repository.specification;

import com.pcverse.entity.AttributeOption;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class AttributeOptionSpecification {

    public static Specification<AttributeOption> belongsToAttributeDefinition(String attributeDefinitionId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("attributeDefinition").get("id"), attributeDefinitionId);
    }

    public static Specification<AttributeOption> hasKeyword(String keyword) {

        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            String escapedKeyword = escapeLikePattern(normalizedKeyword);
            String pattern = "%" + escapedKeyword + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern, '\\'),
                    cb.like(cb.lower(root.get("label")), pattern, '\\')
            );
        };
    }

    public static Specification<AttributeOption> hasActive(Boolean active) {

        return (root, query, cb) -> {

            if (active == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("active"), active);
        };
    }


}
