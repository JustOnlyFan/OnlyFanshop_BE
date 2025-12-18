package com.example.onlyfanshop_be.dto.request;

import com.example.onlyfanshop_be.enums.CategoryType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for product filtering requests.
 * Supports multi-criteria filtering including categories, brands, price range, tags, and accessory compatibility.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {
    
    /**
     * List of category IDs to filter by.
     * Products must belong to at least one of these categories (or their subcategories).
     */
    private List<Integer> categoryIds;
    
    /**
     * List of category types to filter by.
     * Products must have at least one category of these types.
     */
    private List<CategoryType> categoryTypes;
    
    /**
     * List of brand IDs to filter by.
     * Products must belong to one of these brands.
     */
    private List<Integer> brandIds;
    
    /**
     * Minimum price filter (inclusive).
     */
    private BigDecimal minPrice;
    
    /**
     * Maximum price filter (inclusive).
     */
    private BigDecimal maxPrice;
    
    /**
     * List of tag codes to filter by.
     * Products must have at least one of these tags (currently active).
     */
    private List<String> tagCodes;
    
    /**
     * Compatible fan type category ID for accessory filtering.
     * Only applicable for accessory products.
     */
    private Integer compatibleFanTypeId;
    
    /**
     * Search query for product name/description.
     */
    private String searchQuery;
    
    /**
     * Field to sort by (e.g., "name", "basePrice", "createdAt").
     */
    private String sortBy;
    
    /**
     * Sort direction ("ASC" or "DESC").
     */
    private String sortDirection;
    
    /**
     * Whether to include subcategories when filtering by category.
     * Default is true.
     */
    @Builder.Default
    private Boolean includeSubcategories = true;
}
