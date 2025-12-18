package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.entity.ProductTag;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DTO for Product with categories grouped by type.
 * Includes tags and compatibility information for accessories.
 * Used for API responses that need full product categorization details.
 * 
 * Requirements: 11.3, 11.4
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductWithCategoriesDTO {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal basePrice;
    private String shortDescription;
    private BrandDTO brand;
    
    /**
     * Categories grouped by CategoryType.
     * Each key is a CategoryType, and the value is a list of categories of that type.
     */
    @Builder.Default
    private Map<CategoryType, List<CategoryDTO>> categoriesByType = new EnumMap<>(CategoryType.class);
    
    /**
     * List of tags associated with this product.
     */
    @Builder.Default
    private List<TagDTO> tags = new ArrayList<>();
    
    /**
     * Compatibility information for accessory products.
     * Only populated for products that are accessories.
     * Uses AccessoryCompatibilityDTO for full compatibility information display.
     */
    @Builder.Default
    private List<AccessoryCompatibilityDTO> compatibility = new ArrayList<>();
    
    /**
     * Creates ProductWithCategoriesDTO from a Product entity.
     * Note: This method only sets basic product fields.
     * Categories, tags, and compatibility must be set separately.
     * 
     * @param product the Product entity
     * @return ProductWithCategoriesDTO with basic fields populated
     */
    public static ProductWithCategoriesDTO fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        
        BrandDTO brandDTO = null;
        if (product.getBrand() != null) {
            brandDTO = BrandDTO.builder()
                    .brandID(product.getBrand().getId())
                    .name(product.getBrand().getName())
                    .description(product.getBrand().getDescription())
                    .imageURL(product.getBrand().getLogoUrl())
                    .isActive(true) // Brand is always active in new schema
                    .build();
        }
        
        return ProductWithCategoriesDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .basePrice(product.getBasePrice())
                .shortDescription(product.getShortDescription())
                .brand(brandDTO)
                .categoriesByType(new EnumMap<>(CategoryType.class))
                .tags(new ArrayList<>())
                .compatibility(new ArrayList<>())
                .build();
    }
    
    /**
     * Creates ProductWithCategoriesDTO from a Product entity with all related data.
     * 
     * @param product the Product entity
     * @param productCategories list of ProductCategory entities for this product
     * @param productTags list of ProductTag entities for this product
     * @param compatibilities list of AccessoryCompatibility entities for this product
     * @return fully populated ProductWithCategoriesDTO
     */
    public static ProductWithCategoriesDTO fromEntityWithRelations(
            Product product,
            List<ProductCategory> productCategories,
            List<ProductTag> productTags,
            List<AccessoryCompatibility> compatibilities) {
        
        ProductWithCategoriesDTO dto = fromEntity(product);
        if (dto == null) {
            return null;
        }
        
        // Group categories by type
        if (productCategories != null && !productCategories.isEmpty()) {
            Map<CategoryType, List<CategoryDTO>> grouped = productCategories.stream()
                    .filter(pc -> pc.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            pc -> pc.getCategory().getCategoryType(),
                            () -> new EnumMap<>(CategoryType.class),
                            Collectors.mapping(
                                    pc -> CategoryDTO.fromEntity(pc.getCategory()),
                                    Collectors.toList()
                            )
                    ));
            dto.setCategoriesByType(grouped);
        }
        
        // Convert tags
        if (productTags != null && !productTags.isEmpty()) {
            List<TagDTO> tagDTOs = productTags.stream()
                    .filter(pt -> pt.getTag() != null)
                    .map(pt -> TagDTO.fromEntity(pt.getTag()))
                    .collect(Collectors.toList());
            dto.setTags(tagDTOs);
        }
        
        // Convert compatibility information
        if (compatibilities != null && !compatibilities.isEmpty()) {
            List<AccessoryCompatibilityDTO> compatDTOs = compatibilities.stream()
                    .map(AccessoryCompatibilityDTO::fromEntity)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setCompatibility(compatDTOs);
        }
        
        return dto;
    }
    
    /**
     * Adds a category to the appropriate type group.
     * 
     * @param categoryDTO the category to add
     */
    public void addCategory(CategoryDTO categoryDTO) {
        if (categoryDTO == null || categoryDTO.getCategoryType() == null) {
            return;
        }
        categoriesByType.computeIfAbsent(categoryDTO.getCategoryType(), k -> new ArrayList<>())
                .add(categoryDTO);
    }
    
    /**
     * Gets categories of a specific type.
     * 
     * @param type the CategoryType to retrieve
     * @return list of categories of the specified type, or empty list if none
     */
    public List<CategoryDTO> getCategoriesOfType(CategoryType type) {
        return categoriesByType.getOrDefault(type, Collections.emptyList());
    }
    
    /**
     * Checks if this product has any categories of the specified type.
     * 
     * @param type the CategoryType to check
     * @return true if the product has at least one category of the specified type
     */
    public boolean hasCategoryOfType(CategoryType type) {
        List<CategoryDTO> categories = categoriesByType.get(type);
        return categories != null && !categories.isEmpty();
    }
    
    /**
     * Checks if this product has the required category type (FAN_TYPE or ACCESSORY_TYPE).
     * 
     * @return true if the product has at least one FAN_TYPE or ACCESSORY_TYPE category
     */
    public boolean hasRequiredCategoryType() {
        return hasCategoryOfType(CategoryType.FAN_TYPE) || hasCategoryOfType(CategoryType.ACCESSORY_TYPE);
    }
    
    /**
     * Gets all categories as a flat list.
     * 
     * @return list of all categories regardless of type
     */
    public List<CategoryDTO> getAllCategories() {
        return categoriesByType.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the total number of categories assigned to this product.
     * 
     * @return total category count
     */
    public int getTotalCategoryCount() {
        return categoriesByType.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Checks if this product is an accessory (has ACCESSORY_TYPE category).
     * 
     * @return true if this is an accessory product
     */
    public boolean isAccessory() {
        return hasCategoryOfType(CategoryType.ACCESSORY_TYPE);
    }
    
    /**
     * Checks if this product has compatibility information.
     * 
     * @return true if compatibility information is available
     */
    public boolean hasCompatibilityInfo() {
        return compatibility != null && !compatibility.isEmpty();
    }
}
