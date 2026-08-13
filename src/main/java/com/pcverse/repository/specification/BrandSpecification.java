package com.pcverse.repository.specification;

import com.pcverse.entity.Brand;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class BrandSpecification {

    public Specification<Brand> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String normalizedKeyword = keyword
                    .strip()
                    .toLowerCase(Locale.ROOT);
            String pattern = "%"
                    + escapeLikePattern(normalizedKeyword)
                    + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            pattern,
                            '\\'
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("slug")),
                            pattern,
                            '\\'
                    )
            );
        };
    }

    public Specification<Brand> hasActive(Boolean active) {
        return (root, query, criteriaBuilder) -> active == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("active"), active);
    }
}
