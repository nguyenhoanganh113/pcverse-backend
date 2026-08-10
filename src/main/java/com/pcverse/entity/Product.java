package com.pcverse.entity;

import com.pcverse.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Check;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Check(constraints = "price >= 0 AND stock_quantity >= 0")
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_slug",
                        columnNames = "slug"
                ),
                @UniqueConstraint(
                        name = "uk_products_sku",
                        columnNames = "sku"
                )
        },
        indexes = {
                @Index(
                        name = "idx_products_category_id",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_products_brand_id",
                        columnList = "brand_id"
                ),
                @Index(
                        name = "idx_products_status",
                        columnList = "product_status"
                ),
                @Index(
                        name = "idx_products_price",
                        columnList = "price"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String slug;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal price;

    @Builder.Default
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    // Có cho phép đặt khi hết hàng hay không ?
    @Builder.Default
    @Column(name = "allow_backorder", nullable = false)
    private boolean allowBackorder = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ProductImage> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "product_status",
            nullable = false,
            length = 30
    )
    private ProductStatus productStatus = ProductStatus.INACTIVE;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Transient
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public void addAttributeValue(ProductAttributeValue attributeValue) {
        attributeValues.add(attributeValue);
        attributeValue.setProduct(this);
    }

    public void removeAttributeValue(ProductAttributeValue attributeValue) {
        attributeValues.remove(attributeValue);
        attributeValue.setProduct(null);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Product other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
