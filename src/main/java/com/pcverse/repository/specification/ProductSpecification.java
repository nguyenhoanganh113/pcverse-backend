package com.pcverse.repository.specification;

import com.pcverse.entity.Brand;
import com.pcverse.entity.Category;
import com.pcverse.entity.Product;
import com.pcverse.entity.ProductAttributeValue;
import com.pcverse.enums.ProductStatus;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.pcverse.utils.SpecificationUtils.escapeLikePattern;

@UtilityClass
public class ProductSpecification {

    public Specification<Product> hasKeyword(String keyword) {
        if(StringUtils.isBlank(keyword)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            String pattern = "%" + escapeLikePattern(normalizedKeyword) + "%";

            return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern, '\\'),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), pattern, '\\')
            );
        }
    }

    public Specification<Product> hasPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        if(minPrice == null && maxPrice == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        else if(minPrice != null && maxPrice == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .greaterThanOrEqualTo(root.get("price"), minPrice);
        } else if(minPrice == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .lessThanOrEqualTo(root.get("price"), maxPrice);
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .between(root.get("price"), minPrice, maxPrice);
        }
    }

    public Specification<Product> hasStatus(ProductStatus status) {
        if(status == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("productStatus"), status);
        }
    }

    public Specification<Product> isPubliclyVisible() {
        return (root, query, criteriaBuilder) -> {
            Join<Product, Category> categoryJoin = root.join("category");
            Join<Product, Brand> brandJoin = root.join("brand");

            return criteriaBuilder.and(
                    criteriaBuilder.equal(
                            root.get("productStatus"),
                            ProductStatus.ACTIVE
                    ),
                    criteriaBuilder.isTrue(categoryJoin.get("active")),
                    criteriaBuilder.isTrue(brandJoin.get("active"))
            );
        };
    }

    public Specification<Product> inStock(Boolean inStock) {
        if(inStock == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else if(inStock) {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .greaterThanOrEqualTo(root.get("stockQuantity"), 1);
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("stockQuantity"), 0);
        }
    }

    public Specification<Product> hasCategory(String categoryId) {
        if(StringUtils.isBlank(categoryId)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, query, criteriaBuilder) -> {
                Join<Product, Category> categoryJoin = root.join("category");
                return criteriaBuilder.equal(categoryJoin.get("id"), categoryId);
            };
        }
    }

    public Specification<Product> hasBrand(String brandId) {
        if(StringUtils.isBlank(brandId)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, query, criteriaBuilder) -> {
                Join<Product, Brand> brandJoin = root.join("brand");
                return criteriaBuilder.equal(brandJoin.get("id"), brandId);
            };
        }
    }

    public Specification<Product> hasAttributeOptions(
            Map<String, Set<String>> optionIdsByDefinitionId
    ) {
        if (optionIdsByDefinitionId == null
                || optionIdsByDefinitionId.isEmpty()) {
            return (root, query, criteriaBuilder) ->
                    criteriaBuilder.conjunction();
        }

        return (root, query, criteriaBuilder) -> {
            Predicate[] definitionPredicates = optionIdsByDefinitionId
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        Subquery<Integer> subquery = query.subquery(
                                Integer.class
                        );
                        Root<ProductAttributeValue> valueRoot = subquery.from(
                                ProductAttributeValue.class
                        );

                        subquery.select(criteriaBuilder.literal(1));
                        subquery.where(
                                criteriaBuilder.equal(
                                        valueRoot.get("product").get("id"),
                                        root.get("id")
                                ),
                                criteriaBuilder.equal(
                                        valueRoot
                                                .get("attributeDefinition")
                                                .get("id"),
                                        entry.getKey()
                                ),
                                valueRoot
                                        .get("attributeOption")
                                        .get("id")
                                        .in(entry.getValue())
                        );

                        return criteriaBuilder.exists(subquery);
                    })
                    .toArray(Predicate[]::new);

            return criteriaBuilder.and(definitionPredicates);
        };
    }

}
