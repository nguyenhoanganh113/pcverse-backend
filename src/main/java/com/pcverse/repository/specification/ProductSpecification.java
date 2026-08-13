package com.pcverse.repository.specification;

import com.pcverse.entity.Brand;
import com.pcverse.entity.Category;
import com.pcverse.entity.Product;
import com.pcverse.enums.ProductStatus;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.criteria.Join;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

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

}
