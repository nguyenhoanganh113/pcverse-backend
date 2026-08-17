package com.pcverse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Entity
@Table(
        name = "attribute_options",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_option_code",
                        columnNames = {
                                "attribute_definition_id",
                                "code"
                        }
                )
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttributeOption extends AbstractAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_definition_id", nullable = false)
    private AttributeDefinition attributeDefinition;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String label;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    @PreUpdate
    void normalizeCode() {
        if (code != null) {
            code = code.strip().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AttributeOption other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
