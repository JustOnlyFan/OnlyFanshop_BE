package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Product-Category serialization round-trip.
 * 
 * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
 * **Validates: Requirements 11.3, 11.4**
 */
class ProductCategorySerializationPropertyTest {

    private final ObjectMapper objectMapper;

    public ProductCategorySerializationPropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Ignore unknown properties during deserialization (e.g., computed properties like isAccessory, allCategories)
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
     * **Validates: Requirements 11.3, 11.4**
     * 
     * Property: For any valid ProductWithCategoriesDTO with multiple categories,
     * serializing to JSON and deserializing back SHALL preserve all category assignments grouped by type.
     */
    @Property(tries = 100)
    void productWithCategoriesSerializationRoundTrip(
            @ForAll("validProductWithCategoriesDTOs") ProductWithCategoriesDTO original) throws JsonProcessingException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize back to ProductWithCategoriesDTO
        ProductWithCategoriesDTO deserialized = objectMapper.readValue(json, ProductWithCategoriesDTO.class);
        
        // Verify basic product fields are preserved
        assertThat(deserialized.getId()).isEqualTo(original.getId());
        assertThat(deserialized.getName()).isEqualTo(original.getName());
        assertThat(deserialized.getSlug()).isEqualTo(original.getSlug());
        assertThat(deserialized.getBasePrice()).isEqualByComparingTo(original.getBasePrice());
        assertThat(deserialized.getShortDescription()).isEqualTo(original.getShortDescription());
        
        // Verify categories by type are preserved
        assertCategoriesByTypeEquals(original.getCategoriesByType(), deserialized.getCategoriesByType());
    }


    /**
     * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
     * **Validates: Requirements 11.3, 11.4**
     * 
     * Property: For any ProductWithCategoriesDTO, the JSON representation SHALL include
     * all assigned categories grouped by their CategoryType.
     */
    @Property(tries = 100)
    void jsonIncludesCategoriesGroupedByType(
            @ForAll("productWithMultipleCategoryTypes") ProductWithCategoriesDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        
        // Verify categoriesByType is present in JSON
        assertThat(json).contains("categoriesByType");
        
        // Verify each category type that has categories is present
        for (CategoryType type : original.getCategoriesByType().keySet()) {
            List<CategoryDTO> categories = original.getCategoriesByType().get(type);
            if (categories != null && !categories.isEmpty()) {
                assertThat(json).contains(type.name());
            }
        }
    }

    /**
     * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
     * **Validates: Requirements 11.3, 11.4**
     * 
     * Property: For any ProductWithCategoriesDTO with tags, serializing and deserializing
     * SHALL preserve all tag information.
     */
    @Property(tries = 100)
    void tagsPreservedAfterRoundTrip(
            @ForAll("productWithTags") ProductWithCategoriesDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        ProductWithCategoriesDTO deserialized = objectMapper.readValue(json, ProductWithCategoriesDTO.class);
        
        // Verify tags are preserved
        assertThat(deserialized.getTags()).hasSameSizeAs(original.getTags());
        
        for (int i = 0; i < original.getTags().size(); i++) {
            TagDTO originalTag = original.getTags().get(i);
            TagDTO deserializedTag = deserialized.getTags().get(i);
            
            assertThat(deserializedTag.getId()).isEqualTo(originalTag.getId());
            assertThat(deserializedTag.getCode()).isEqualTo(originalTag.getCode());
            assertThat(deserializedTag.getDisplayName()).isEqualTo(originalTag.getDisplayName());
            assertThat(deserializedTag.getBadgeColor()).isEqualTo(originalTag.getBadgeColor());
            assertThat(deserializedTag.getDisplayOrder()).isEqualTo(originalTag.getDisplayOrder());
        }
    }

    /**
     * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
     * **Validates: Requirements 11.3, 11.4**
     * 
     * Property: For any ProductWithCategoriesDTO with compatibility information,
     * serializing and deserializing SHALL preserve all compatibility entries.
     */
    @Property(tries = 100)
    void compatibilityPreservedAfterRoundTrip(
            @ForAll("productWithCompatibility") ProductWithCategoriesDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        ProductWithCategoriesDTO deserialized = objectMapper.readValue(json, ProductWithCategoriesDTO.class);
        
        // Verify compatibility info is preserved
        assertThat(deserialized.getCompatibility()).hasSameSizeAs(original.getCompatibility());
        
        for (int i = 0; i < original.getCompatibility().size(); i++) {
            AccessoryCompatibilityDTO originalCompat = original.getCompatibility().get(i);
            AccessoryCompatibilityDTO deserializedCompat = deserialized.getCompatibility().get(i);
            
            assertThat(deserializedCompat.getId()).isEqualTo(originalCompat.getId());
            assertThat(deserializedCompat.getAccessoryProductId()).isEqualTo(originalCompat.getAccessoryProductId());
            assertThat(deserializedCompat.getAccessoryProductName()).isEqualTo(originalCompat.getAccessoryProductName());
            assertThat(deserializedCompat.getCompatibleFanTypeId()).isEqualTo(originalCompat.getCompatibleFanTypeId());
            assertThat(deserializedCompat.getCompatibleFanTypeName()).isEqualTo(originalCompat.getCompatibleFanTypeName());
            assertThat(deserializedCompat.getCompatibleBrandId()).isEqualTo(originalCompat.getCompatibleBrandId());
            assertThat(deserializedCompat.getCompatibleBrandName()).isEqualTo(originalCompat.getCompatibleBrandName());
            assertThat(deserializedCompat.getCompatibleModel()).isEqualTo(originalCompat.getCompatibleModel());
            assertThat(deserializedCompat.getNotes()).isEqualTo(originalCompat.getNotes());
        }
    }

    /**
     * **Feature: expanded-category-system, Property 16: Product-Category Serialization Round-Trip**
     * **Validates: Requirements 11.3, 11.4**
     * 
     * Property: For any ProductWithCategoriesDTO with brand information,
     * serializing and deserializing SHALL preserve the brand data.
     */
    @Property(tries = 100)
    void brandPreservedAfterRoundTrip(
            @ForAll("productWithBrand") ProductWithCategoriesDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        ProductWithCategoriesDTO deserialized = objectMapper.readValue(json, ProductWithCategoriesDTO.class);
        
        // Verify brand is preserved
        assertThat(deserialized.getBrand()).isNotNull();
        assertThat(deserialized.getBrand().getBrandID()).isEqualTo(original.getBrand().getBrandID());
        assertThat(deserialized.getBrand().getName()).isEqualTo(original.getBrand().getName());
        assertThat(deserialized.getBrand().getDescription()).isEqualTo(original.getBrand().getDescription());
        assertThat(deserialized.getBrand().getImageURL()).isEqualTo(original.getBrand().getImageURL());
    }


    /**
     * Helper method to assert that two categoriesByType maps are equal.
     */
    private void assertCategoriesByTypeEquals(
            Map<CategoryType, List<CategoryDTO>> expected,
            Map<CategoryType, List<CategoryDTO>> actual) {
        
        // Check all expected types are present
        for (CategoryType type : expected.keySet()) {
            List<CategoryDTO> expectedCategories = expected.get(type);
            List<CategoryDTO> actualCategories = actual.get(type);
            
            if (expectedCategories == null || expectedCategories.isEmpty()) {
                assertThat(actualCategories == null || actualCategories.isEmpty()).isTrue();
            } else {
                assertThat(actualCategories).isNotNull();
                assertThat(actualCategories).hasSameSizeAs(expectedCategories);
                
                for (int i = 0; i < expectedCategories.size(); i++) {
                    assertCategoryEquals(expectedCategories.get(i), actualCategories.get(i));
                }
            }
        }
    }

    /**
     * Helper method to assert that two CategoryDTOs are equal.
     */
    private void assertCategoryEquals(CategoryDTO expected, CategoryDTO actual) {
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSlug()).isEqualTo(expected.getSlug());
        assertThat(actual.getCategoryType()).isEqualTo(expected.getCategoryType());
        assertThat(actual.getParentId()).isEqualTo(expected.getParentId());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getIconUrl()).isEqualTo(expected.getIconUrl());
        assertThat(actual.getDisplayOrder()).isEqualTo(expected.getDisplayOrder());
        assertThat(actual.getIsActive()).isEqualTo(expected.getIsActive());
    }

    // ==================== Arbitrary Providers ====================

    @Provide
    Arbitrary<ProductWithCategoriesDTO> validProductWithCategoriesDTOs() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(String::toLowerCase),
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(100), BigDecimal.valueOf(100000000)),
                Arbitraries.strings().alpha().ofMaxLength(200).injectNull(0.3)
        ).as((id, name, slug, price, description) -> {
            ProductWithCategoriesDTO dto = ProductWithCategoriesDTO.builder()
                    .id(id)
                    .name(name)
                    .slug(slug)
                    .basePrice(price)
                    .shortDescription(description)
                    .categoriesByType(new EnumMap<>(CategoryType.class))
                    .tags(new ArrayList<>())
                    .compatibility(new ArrayList<>())
                    .build();
            
            // Add categories from multiple types
            addRandomCategories(dto);
            
            return dto;
        });
    }

    @Provide
    Arbitrary<ProductWithCategoriesDTO> productWithMultipleCategoryTypes() {
        return validProductWithCategoriesDTOs().filter(dto -> 
                dto.getCategoriesByType().size() >= 2);
    }

    @Provide
    Arbitrary<ProductWithCategoriesDTO> productWithTags() {
        return validProductWithCategoriesDTOs().map(dto -> {
            // Add 1-5 tags
            int numTags = new Random().nextInt(5) + 1;
            List<TagDTO> tags = new ArrayList<>();
            for (int i = 0; i < numTags; i++) {
                tags.add(generateRandomTag(i));
            }
            dto.setTags(tags);
            return dto;
        });
    }

    @Provide
    Arbitrary<ProductWithCategoriesDTO> productWithCompatibility() {
        return validProductWithCategoriesDTOs().map(dto -> {
            // Add 1-3 compatibility entries
            int numCompat = new Random().nextInt(3) + 1;
            List<AccessoryCompatibilityDTO> compatList = new ArrayList<>();
            for (int i = 0; i < numCompat; i++) {
                compatList.add(generateRandomCompatibility(dto.getId(), i));
            }
            dto.setCompatibility(compatList);
            return dto;
        });
    }

    @Provide
    Arbitrary<ProductWithCategoriesDTO> productWithBrand() {
        return validProductWithCategoriesDTOs().map(dto -> {
            dto.setBrand(generateRandomBrand());
            return dto;
        });
    }


    // ==================== Helper Methods for Data Generation ====================

    private void addRandomCategories(ProductWithCategoriesDTO dto) {
        Random random = new Random();
        CategoryType[] types = CategoryType.values();
        
        // Add categories from 2-4 different types
        int numTypes = random.nextInt(3) + 2;
        Set<CategoryType> selectedTypes = new HashSet<>();
        
        while (selectedTypes.size() < numTypes) {
            selectedTypes.add(types[random.nextInt(types.length)]);
        }
        
        for (CategoryType type : selectedTypes) {
            // Add 1-3 categories per type
            int numCategories = random.nextInt(3) + 1;
            List<CategoryDTO> categories = new ArrayList<>();
            
            for (int i = 0; i < numCategories; i++) {
                categories.add(generateRandomCategory(type, i));
            }
            
            dto.getCategoriesByType().put(type, categories);
        }
    }

    private CategoryDTO generateRandomCategory(CategoryType type, int index) {
        Random random = new Random();
        return CategoryDTO.builder()
                .id(random.nextInt(10000) + 1)
                .name("Category" + random.nextInt(1000))
                .slug("category-" + random.nextInt(1000))
                .categoryType(type)
                .parentId(random.nextBoolean() ? random.nextInt(1000) + 1 : null)
                .description(random.nextBoolean() ? "Description " + random.nextInt(100) : null)
                .iconUrl(random.nextBoolean() ? "http://example.com/icon" + random.nextInt(100) + ".png" : null)
                .displayOrder(random.nextInt(100))
                .isActive(random.nextBoolean())
                .children(new ArrayList<>())
                .build();
    }

    private TagDTO generateRandomTag(int index) {
        Random random = new Random();
        String[] codes = {"NEW", "BESTSELLER", "SALE", "PREMIUM", "IMPORTED", "AUTHENTIC"};
        String code = codes[index % codes.length] + random.nextInt(100);
        
        return TagDTO.builder()
                .id(random.nextInt(10000) + 1)
                .code(code)
                .displayName("Tag " + code)
                .badgeColor("#" + String.format("%06x", random.nextInt(0xFFFFFF)))
                .displayOrder(random.nextInt(100))
                .build();
    }

    private AccessoryCompatibilityDTO generateRandomCompatibility(Long accessoryProductId, int index) {
        Random random = new Random();
        return AccessoryCompatibilityDTO.builder()
                .id((long) (random.nextInt(10000) + 1))
                .accessoryProductId(accessoryProductId)
                .accessoryProductName("Accessory Product " + random.nextInt(100))
                .compatibleFanTypeId(random.nextInt(100) + 1)
                .compatibleFanTypeName("FanType" + random.nextInt(100))
                .compatibleBrandId(random.nextInt(50) + 1)
                .compatibleBrandName("Brand" + random.nextInt(50))
                .compatibleModel("Model-" + random.nextInt(1000))
                .notes(random.nextBoolean() ? "Notes for compatibility " + index : null)
                .build();
    }

    private BrandDTO generateRandomBrand() {
        Random random = new Random();
        return BrandDTO.builder()
                .brandID(random.nextInt(1000) + 1)
                .name("Brand" + random.nextInt(100))
                .description("Brand description " + random.nextInt(100))
                .imageURL("http://example.com/brand" + random.nextInt(100) + ".png")
                .isActive(true)
                .build();
    }
}
