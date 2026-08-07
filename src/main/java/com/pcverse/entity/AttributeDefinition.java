package com.pcverse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "attribute_definitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_definitions_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeDefinition extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "attributeDefinition")
    private List<ProductAttributeValue> productAttributeValues = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "attributeDefinition", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<AttributeOption> attributeOptions = new ArrayList<>();

    @OneToMany(mappedBy = "attributeDefinition")
    @Builder.Default
    private List<CategoryAttribute> categoryAttributes = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttributeDefinition other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void addAttributeOption(AttributeOption attributeOption) {
        attributeOptions.add(attributeOption);
        attributeOption.setAttributeDefinition(this);
    }

    public void removeAttributeOption(AttributeOption attributeOption) {
        attributeOptions.remove(attributeOption);
        attributeOption.setAttributeDefinition(null);
    }

}
