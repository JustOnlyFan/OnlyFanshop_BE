package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories",
    indexes = {@Index(name = "idx_categories_parent_id", columnList = "parent_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "parent_id", columnDefinition = "INT UNSIGNED")
    private Integer parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    @JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent")
    @JsonIgnore
    private List<Category> children;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Legacy fields for backward compatibility
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getCategoryID() {
        return id;
    }

    @Transient
    public String getCategoryName() {
        return name;
    }

    @Transient
    public boolean isActive() {
        return true; // Always active in new schema
    }

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<Product> products;
}
