package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Category with hierarchy support.
 * Includes all category fields including type, parent reference, and children list.
 * Supports serialization/deserialization for API responses.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDTO {
    private Integer id;
    private String name;
    private String slug;
    private CategoryType categoryType;
    private Integer parentId;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;
    
    /**
     * List of child categories for hierarchy representation.
     */
    @Builder.Default
    private List<CategoryDTO> children = new ArrayList<>();
    
    /**
     * Converts a Category entity to CategoryDTO without children.
     * @param category the Category entity
     * @return CategoryDTO representation
     */
    public static CategoryDTO fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .categoryType(category.getCategoryType())
                .parentId(category.getParentId())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .children(new ArrayList<>())
                .build();
    }
    
    /**
     * Converts a Category entity to CategoryDTO with children (recursive).
     * @param category the Category entity
     * @return CategoryDTO representation with children
     */
    public static CategoryDTO fromEntityWithChildren(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDTO dto = fromEntity(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            dto.setChildren(category.getChildren().stream()
                    .map(CategoryDTO::fromEntityWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
    
    /**
     * Converts this DTO to a Category entity.
     * Note: This does not set the parent or children relationships.
     * @return Category entity
     */
    public Category toEntity() {
        return Category.builder()
                .id(this.id)
                .name(this.name)
                .slug(this.slug)
                .categoryType(this.categoryType)
                .parentId(this.parentId)
                .description(this.description)
                .iconUrl(this.iconUrl)
                .displayOrder(this.displayOrder)
                .isActive(this.isActive)
                .build();
    }
    
    /**
     * Creates a simple CategoryDTO with only id and name (for backward compatibility).
     * @param id the category ID
     * @param name the category name
     * @return simple CategoryDTO
     */
    public static CategoryDTO simple(Integer id, String name) {
        return CategoryDTO.builder()
                .id(id)
                .name(name)
                .build();
    }
}
