package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.CategoryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.jqwik.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Category serialization round-trip.
 * 
 * **Feature: expanded-category-system, Property 15: Category Serialization Round-Trip**
 * **Validates: Requirements 11.1, 11.2**
 */
class CategorySerializationPropertyTest {

    private final ObjectMapper objectMapper;

    public CategorySerializationPropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * **Feature: expanded-category-system, Property 15: Category Serialization Round-Trip**
     * **Validates: Requirements 11.1, 11.2**
     * 
     * Property: For any valid CategoryDTO, serializing to JSON and deserializing back
     * SHALL produce an equivalent CategoryDTO with the same field values.
     */
    @Property(tries = 100)
    void categoryDtoSerializationRoundTrip(
            @ForAll("validCategoryDTOs") CategoryDTO original) throws JsonProcessingException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize back to CategoryDTO
        CategoryDTO deserialized = objectMapper.readValue(json, CategoryDTO.class);
        
        // Verify all fields are preserved
        assertThat(deserialized.getId()).isEqualTo(original.getId());
        assertThat(deserialized.getName()).isEqualTo(original.getName());
        assertThat(deserialized.getSlug()).isEqualTo(original.getSlug());
        assertThat(deserialized.getCategoryType()).isEqualTo(original.getCategoryType());
        assertThat(deserialized.getParentId()).isEqualTo(original.getParentId());
        assertThat(deserialized.getDescription()).isEqualTo(original.getDescription());
        assertThat(deserialized.getIconUrl()).isEqualTo(original.getIconUrl());
        assertThat(deserialized.getDisplayOrder()).isEqualTo(original.getDisplayOrder());
        assertThat(deserialized.getIsActive()).isEqualTo(original.getIsActive());
    }

    /**
     * **Feature: expanded-category-system, Property 15: Category Serialization Round-Trip**
     * **Validates: Requirements 11.1, 11.2**
     * 
     * Property: For any valid CategoryDTO with children, serializing to JSON and deserializing back
     * SHALL preserve the complete hierarchy structure including all children.
     */
    @Property(tries = 100)
    void categoryDtoWithChildrenSerializationRoundTrip(
            @ForAll("categoryDTOsWithChildren") CategoryDTO original) throws JsonProcessingException {
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize back to CategoryDTO
        CategoryDTO deserialized = objectMapper.readValue(json, CategoryDTO.class);
        
        // Verify hierarchy is preserved
        assertCategoryHierarchyEquals(original, deserialized);
    }

    /**
     * **Feature: expanded-category-system, Property 15: Category Serialization Round-Trip**
     * **Validates: Requirements 11.1, 11.2**
     * 
     * Property: For any CategoryDTO, the JSON representation SHALL include the categoryType field.
     */
    @Property(tries = 100)
    void jsonIncludesCategoryType(
            @ForAll("validCategoryDTOs") CategoryDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        
        // Verify categoryType is present in JSON
        assertThat(json).contains("categoryType");
        if (original.getCategoryType() != null) {
            assertThat(json).contains(original.getCategoryType().name());
        }
    }

    /**
     * **Feature: expanded-category-system, Property 15: Category Serialization Round-Trip**
     * **Validates: Requirements 11.1, 11.2**
     * 
     * Property: For any CategoryDTO with a parent reference, the parentId SHALL be preserved
     * after serialization round-trip.
     */
    @Property(tries = 100)
    void parentIdPreservedAfterRoundTrip(
            @ForAll("categoryDTOsWithParent") CategoryDTO original) throws JsonProcessingException {
        
        String json = objectMapper.writeValueAsString(original);
        CategoryDTO deserialized = objectMapper.readValue(json, CategoryDTO.class);
        
        assertThat(deserialized.getParentId()).isEqualTo(original.getParentId());
    }

    /**
     * Recursively asserts that two CategoryDTO hierarchies are equal.
     */
    private void assertCategoryHierarchyEquals(CategoryDTO expected, CategoryDTO actual) {
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSlug()).isEqualTo(expected.getSlug());
        assertThat(actual.getCategoryType()).isEqualTo(expected.getCategoryType());
        assertThat(actual.getParentId()).isEqualTo(expected.getParentId());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getIconUrl()).isEqualTo(expected.getIconUrl());
        assertThat(actual.getDisplayOrder()).isEqualTo(expected.getDisplayOrder());
        assertThat(actual.getIsActive()).isEqualTo(expected.getIsActive());
        
        // Check children
        List<CategoryDTO> expectedChildren = expected.getChildren();
        List<CategoryDTO> actualChildren = actual.getChildren();
        
        if (expectedChildren == null || expectedChildren.isEmpty()) {
            assertThat(actualChildren == null || actualChildren.isEmpty()).isTrue();
        } else {
            assertThat(actualChildren).isNotNull();
            assertThat(actualChildren).hasSameSizeAs(expectedChildren);
            
            for (int i = 0; i < expectedChildren.size(); i++) {
                assertCategoryHierarchyEquals(expectedChildren.get(i), actualChildren.get(i));
            }
        }
    }

    @Provide
    Arbitrary<CategoryDTO> validCategoryDTOs() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 10000);
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
        Arbitrary<String> slugs = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(String::toLowerCase);
        Arbitrary<CategoryType> types = Arbitraries.of(CategoryType.values());
        Arbitrary<Integer> parentIds = Arbitraries.integers().between(1, 10000).injectNull(0.3);
        Arbitrary<String> descriptions = Arbitraries.strings().alpha().ofMaxLength(200).injectNull(0.3);
        Arbitrary<String> iconUrls = Arbitraries.strings().alpha().ofMaxLength(100).injectNull(0.5);
        Arbitrary<Integer> displayOrders = Arbitraries.integers().between(0, 100);
        Arbitrary<Boolean> isActives = Arbitraries.of(true, false);
        
        // Combine first 8 parameters
        return Combinators.combine(ids, names, slugs, types, parentIds, descriptions, iconUrls, displayOrders)
                .flatAs((id, name, slug, type, parentId, description, iconUrl, displayOrder) ->
                        isActives.map(isActive ->
                                CategoryDTO.builder()
                                        .id(id)
                                        .name(name)
                                        .slug(slug)
                                        .categoryType(type)
                                        .parentId(parentId)
                                        .description(description)
                                        .iconUrl(iconUrl)
                                        .displayOrder(displayOrder)
                                        .isActive(isActive)
                                        .children(new ArrayList<>())
                                        .build()
                        )
                );
    }

    @Provide
    Arbitrary<CategoryDTO> categoryDTOsWithChildren() {
        return validCategoryDTOs().flatMap(parent -> {
            // Generate 0-3 children
            return Arbitraries.integers().between(0, 3).flatMap(numChildren -> {
                if (numChildren == 0) {
                    return Arbitraries.just(parent);
                }
                return validCategoryDTOs()
                        .list()
                        .ofSize(numChildren)
                        .map(children -> {
                            // Set parent reference for children
                            children.forEach(child -> child.setParentId(parent.getId()));
                            parent.setChildren(children);
                            return parent;
                        });
            });
        });
    }

    @Provide
    Arbitrary<CategoryDTO> categoryDTOsWithParent() {
        Arbitrary<Integer> ids = Arbitraries.integers().between(1, 10000);
        Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50);
        Arbitrary<String> slugs = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50).map(String::toLowerCase);
        Arbitrary<CategoryType> types = Arbitraries.of(CategoryType.values());
        Arbitrary<Integer> parentIds = Arbitraries.integers().between(1, 10000); // Always has a parent
        Arbitrary<String> descriptions = Arbitraries.strings().alpha().ofMaxLength(200).injectNull(0.3);
        Arbitrary<String> iconUrls = Arbitraries.strings().alpha().ofMaxLength(100).injectNull(0.5);
        Arbitrary<Integer> displayOrders = Arbitraries.integers().between(0, 100);
        Arbitrary<Boolean> isActives = Arbitraries.of(true, false);
        
        return Combinators.combine(ids, names, slugs, types, parentIds, descriptions, iconUrls, displayOrders)
                .flatAs((id, name, slug, type, parentId, description, iconUrl, displayOrder) ->
                        isActives.map(isActive ->
                                CategoryDTO.builder()
                                        .id(id)
                                        .name(name)
                                        .slug(slug)
                                        .categoryType(type)
                                        .parentId(parentId)
                                        .description(description)
                                        .iconUrl(iconUrl)
                                        .displayOrder(displayOrder)
                                        .isActive(isActive)
                                        .children(new ArrayList<>())
                                        .build()
                        )
                );
    }
}
