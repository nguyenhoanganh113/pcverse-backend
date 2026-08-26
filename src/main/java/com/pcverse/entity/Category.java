package com.pcverse.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Category extends AbstractAuditingEntity {

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, unique = true)
    private String categoryImageUrl;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
