package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.enums.CategoryType;
import lombok.*;

/**
 * DTO for ProductCategory relationship.
 * Used to return category information for a product.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductCategoryDTO {
    private Long id;
    private Long productId;
    private Integer categoryId;
    private String categoryName;
    private String categorySlug;
    private CategoryType categoryType;
    private CategoryDTO category;
    
    public static ProductCategoryDTO fromEntity(ProductCategory entity) {
        if (entity == null) {
            return null;
        }
        
        ProductCategoryDTOBuilder builder = ProductCategoryDTO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .categoryId(entity.getCategoryId());
        
        if (entity.getCategory() != null) {
            builder.categoryName(entity.getCategory().getName())
                   .categorySlug(entity.getCategory().getSlug())
                   .categoryType(entity.getCategory().getCategoryType())
                   .category(CategoryDTO.fromEntity(entity.getCategory()));
        }
        
        return builder.build();
    }
}
