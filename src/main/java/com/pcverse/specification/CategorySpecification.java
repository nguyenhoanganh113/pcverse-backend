package com.pcverse.specification;

import com.pcverse.entity.Category;
import com.pcverse.enums.CategorySearchField;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

// Utility class
@UtilityClass
public class CategorySpecification {

    public Specification<Category> hasKeyword(String keyword) {
        return (root, query, cb) -> {

            if(!StringUtils.hasText(keyword)) {
                return cb.conjunction();
            }

            String normalizedKeyword = keyword
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String pattern = "%"
                    + escapeLikePattern(normalizedKeyword)
                    + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("slug")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\')
            );

        };
    }

    public Specification<Category> hasAttribute(CategorySearchField field, String value, boolean exact) {
        return (root, query, cb) -> {

            if(!StringUtils.hasText(value) || field == null) {
                return cb.conjunction();
            }

            String normalizedValue = value
                    .trim()
                    .toLowerCase(Locale.ROOT);

            String pattern = "%"
                    + escapeLikePattern(normalizedValue)
                    + "%";

            if (exact) {
                return cb.equal(cb.lower(root.get(field.path())), normalizedValue);
            }

            return cb.like(cb.lower(root.get(field.path())), pattern, '\\');
        };
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
