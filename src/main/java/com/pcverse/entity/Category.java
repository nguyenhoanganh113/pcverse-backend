package com.pcverse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_categories_slug",
                columnNames = "slug"
        ),
        indexes = {
                @Index(
                        name = "idx_categories_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<CategoryAttribute> categoryAttributes = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Category other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
