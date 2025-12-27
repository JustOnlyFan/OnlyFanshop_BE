package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Builder.Default
    private List<CategoryDTO> children = new ArrayList<>();

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

    public static CategoryDTO simple(Integer id, String name) {
        return CategoryDTO.builder()
                .id(id)
                .name(name)
                .build();
    }
}
