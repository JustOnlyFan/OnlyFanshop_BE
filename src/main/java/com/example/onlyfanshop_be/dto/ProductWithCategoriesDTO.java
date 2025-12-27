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

    @Builder.Default
    private Map<CategoryType, List<CategoryDTO>> categoriesByType = new EnumMap<>(CategoryType.class);

    @Builder.Default
    private List<TagDTO> tags = new ArrayList<>();

    @Builder.Default
    private List<AccessoryCompatibilityDTO> compatibility = new ArrayList<>();

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

    public void addCategory(CategoryDTO categoryDTO) {
        if (categoryDTO == null || categoryDTO.getCategoryType() == null) {
            return;
        }
        categoriesByType.computeIfAbsent(categoryDTO.getCategoryType(), k -> new ArrayList<>())
                .add(categoryDTO);
    }

    public List<CategoryDTO> getCategoriesOfType(CategoryType type) {
        return categoriesByType.getOrDefault(type, Collections.emptyList());
    }

    public boolean hasCategoryOfType(CategoryType type) {
        List<CategoryDTO> categories = categoriesByType.get(type);
        return categories != null && !categories.isEmpty();
    }

    public boolean hasRequiredCategoryType() {
        return hasCategoryOfType(CategoryType.FAN_TYPE) || hasCategoryOfType(CategoryType.ACCESSORY_TYPE);
    }

    public List<CategoryDTO> getAllCategories() {
        return categoriesByType.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public int getTotalCategoryCount() {
        return categoriesByType.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public boolean isAccessory() {
        return hasCategoryOfType(CategoryType.ACCESSORY_TYPE);
    }

    public boolean hasCompatibilityInfo() {
        return compatibility != null && !compatibility.isEmpty();
    }
}
