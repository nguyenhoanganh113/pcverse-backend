package com.pcverse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address extends AbstractAuditingEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String ward;

    private String streetDetail;

    @Column(name = "is_default")
    private Boolean isDefault = false;

}
