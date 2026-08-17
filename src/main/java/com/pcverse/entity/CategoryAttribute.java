package com.pcverse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "category_attribute",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_category_attribute",
                        columnNames = {
                                "category_id",
                                "attribute_definition_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_category_attributes_attribute_definition",
                        columnList = "attribute_definition_id"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryAttribute extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    // Cho biết là thuộc tính này có bắt buộc phải có giá trị hay không
    /**
     * Có bắt buộc nhập giá trị khi tạo Product không?
     */
    @Builder.Default
    @Column(name = "is_required", nullable = false)
    private boolean required = false;

    // Cho biết là thuộc tính này có thể được sử dụng để lọc sản phẩm hay không
    /**
     * Có xuất hiện trong filterAttributes không?
     */
    @Builder.Default
    @Column(name = "is_filterable", nullable = false)
    private boolean filterable = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Builder.Default
    @Column(name = "is_highlighted", nullable = false)
    private boolean highlighted = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof CategoryAttribute other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
