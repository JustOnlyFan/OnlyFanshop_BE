package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "categories",
    indexes = {
        @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
        @Index(name = "idx_categories_type", columnList = "category_type"),
        @Index(name = "idx_categories_parent_type", columnList = "parent_id, category_type")
    })
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

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    @Builder.Default
    private CategoryType categoryType = CategoryType.FAN_TYPE;

    @Column(name = "parent_id", columnDefinition = "INT UNSIGNED")
    private Integer parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    @JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Category> children;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isActive = true;

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
    public boolean getIsActiveStatus() {
        return isActive != null ? isActive : true;
    }

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<Product> products;

    /**
     * Calculates the depth of this category in the hierarchy.
     * Root categories have depth 1.
     * @return the depth level of this category
     */
    @Transient
    public int getDepth() {
        if (parent == null) {
            return 1;
        }
        return parent.getDepth() + 1;
    }

    /**
     * Checks if this category can have children based on the maximum depth limit.
     * Maximum allowed depth is 3 levels.
     * @return true if this category can have children
     */
    @Transient
    public boolean canHaveChildren() {
        return getDepth() < 3;
    }

    /**
     * Validates that a potential child category has the same type as this category.
     * @param childType the category type of the potential child
     * @return true if the child type matches this category's type
     */
    public boolean isValidChildType(CategoryType childType) {
        return this.categoryType == childType;
    }
}
