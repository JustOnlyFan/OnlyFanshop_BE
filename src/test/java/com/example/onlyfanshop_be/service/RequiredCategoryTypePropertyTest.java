package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for required category type constraint.
 * 
 * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
 * **Validates: Requirements 2.2**
 */
class RequiredCategoryTypePropertyTest {

    /**
     * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
     * **Validates: Requirements 2.2**
     * 
     * Property: For any product, there SHALL exist at least one assigned category 
     * of type FAN_TYPE or ACCESSORY_TYPE.
     * 
     * This test verifies that the validation correctly identifies when required
     * category types are present.
     */
    @Property(tries = 100)
    void productWithFanTypeCategoryPassesValidation(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithFanType") List<TestCategory> categories) {
        
        RequiredCategoryValidator validator = new RequiredCategoryValidator();
        
        // Assign categories to product
        for (TestCategory category : categories) {
            validator.assignCategory(productId, category);
        }
        
        // Validation should pass since FAN_TYPE is included
        assertThat(validator.hasRequiredCategoryType(productId)).isTrue();
    }

    /**
     * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
     * **Validates: Requirements 2.2**
     * 
     * Property: Products with ACCESSORY_TYPE category should also pass validation.
     */
    @Property(tries = 100)
    void productWithAccessoryTypeCategoryPassesValidation(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithAccessoryType") List<TestCategory> categories) {
        
        RequiredCategoryValidator validator = new RequiredCategoryValidator();
        
        // Assign categories to product
        for (TestCategory category : categories) {
            validator.assignCategory(productId, category);
        }
        
        // Validation should pass since ACCESSORY_TYPE is included
        assertThat(validator.hasRequiredCategoryType(productId)).isTrue();
    }


    /**
     * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
     * **Validates: Requirements 2.2**
     * 
     * Property: Products with only non-required category types should fail validation.
     */
    @Property(tries = 100)
    void productWithoutRequiredCategoryTypeFailsValidation(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithoutRequiredTypes") List<TestCategory> categories) {
        
        // Skip if no categories (edge case handled separately)
        Assume.that(!categories.isEmpty());
        
        RequiredCategoryValidator validator = new RequiredCategoryValidator();
        
        // Assign categories to product
        for (TestCategory category : categories) {
            validator.assignCategory(productId, category);
        }
        
        // Validation should fail since no FAN_TYPE or ACCESSORY_TYPE is included
        assertThat(validator.hasRequiredCategoryType(productId)).isFalse();
    }

    /**
     * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
     * **Validates: Requirements 2.2**
     * 
     * Property: Products with no categories should fail validation.
     */
    @Property(tries = 100)
    void productWithNoCategoriesFailsValidation(
            @ForAll @IntRange(min = 1, max = 1000) int productId) {
        
        RequiredCategoryValidator validator = new RequiredCategoryValidator();
        
        // No categories assigned
        
        // Validation should fail
        assertThat(validator.hasRequiredCategoryType(productId)).isFalse();
    }

    /**
     * **Feature: expanded-category-system, Property 7: Required Category Type Constraint**
     * **Validates: Requirements 2.2**
     * 
     * Property: Having both FAN_TYPE and ACCESSORY_TYPE should pass validation.
     */
    @Property(tries = 100)
    void productWithBothRequiredTypesPassesValidation(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithBothRequiredTypes") List<TestCategory> categories) {
        
        RequiredCategoryValidator validator = new RequiredCategoryValidator();
        
        // Assign categories to product
        for (TestCategory category : categories) {
            validator.assignCategory(productId, category);
        }
        
        // Validation should pass
        assertThat(validator.hasRequiredCategoryType(productId)).isTrue();
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesWithFanType() {
        // Generate categories that include at least one FAN_TYPE
        Arbitrary<TestCategory> fanTypeCategory = Arbitraries.integers().between(1, 100)
                .map(id -> new TestCategory(id, CategoryType.FAN_TYPE));
        
        Arbitrary<TestCategory> otherCategory = Combinators.combine(
                Arbitraries.integers().between(101, 200),
                Arbitraries.of(CategoryType.SPACE, CategoryType.PURPOSE, CategoryType.TECHNOLOGY, 
                        CategoryType.PRICE_RANGE, CategoryType.CUSTOMER_TYPE, CategoryType.STATUS)
        ).as(TestCategory::new);
        
        return Combinators.combine(
                fanTypeCategory.list().ofMinSize(1).ofMaxSize(2),
                otherCategory.list().ofMinSize(0).ofMaxSize(3)
        ).as((fanTypes, others) -> {
            List<TestCategory> result = new ArrayList<>(fanTypes);
            result.addAll(others);
            return result;
        });
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesWithAccessoryType() {
        // Generate categories that include at least one ACCESSORY_TYPE
        Arbitrary<TestCategory> accessoryTypeCategory = Arbitraries.integers().between(1, 100)
                .map(id -> new TestCategory(id, CategoryType.ACCESSORY_TYPE));
        
        Arbitrary<TestCategory> otherCategory = Combinators.combine(
                Arbitraries.integers().between(101, 200),
                Arbitraries.of(CategoryType.SPACE, CategoryType.PURPOSE, CategoryType.TECHNOLOGY, 
                        CategoryType.PRICE_RANGE, CategoryType.CUSTOMER_TYPE, CategoryType.STATUS)
        ).as(TestCategory::new);
        
        return Combinators.combine(
                accessoryTypeCategory.list().ofMinSize(1).ofMaxSize(2),
                otherCategory.list().ofMinSize(0).ofMaxSize(3)
        ).as((accessoryTypes, others) -> {
            List<TestCategory> result = new ArrayList<>(accessoryTypes);
            result.addAll(others);
            return result;
        });
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesWithoutRequiredTypes() {
        // Generate categories that do NOT include FAN_TYPE or ACCESSORY_TYPE
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of(CategoryType.SPACE, CategoryType.PURPOSE, CategoryType.TECHNOLOGY, 
                        CategoryType.PRICE_RANGE, CategoryType.CUSTOMER_TYPE, CategoryType.STATUS,
                        CategoryType.ACCESSORY_FUNCTION)
        ).as(TestCategory::new)
        .list().ofMinSize(1).ofMaxSize(5);
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesWithBothRequiredTypes() {
        // Generate categories that include both FAN_TYPE and ACCESSORY_TYPE
        Arbitrary<TestCategory> fanTypeCategory = Arbitraries.integers().between(1, 50)
                .map(id -> new TestCategory(id, CategoryType.FAN_TYPE));
        
        Arbitrary<TestCategory> accessoryTypeCategory = Arbitraries.integers().between(51, 100)
                .map(id -> new TestCategory(id, CategoryType.ACCESSORY_TYPE));
        
        return Combinators.combine(
                fanTypeCategory.list().ofMinSize(1).ofMaxSize(2),
                accessoryTypeCategory.list().ofMinSize(1).ofMaxSize(2)
        ).as((fanTypes, accessoryTypes) -> {
            List<TestCategory> result = new ArrayList<>(fanTypes);
            result.addAll(accessoryTypes);
            return result;
        });
    }

    /**
     * Simple test category class for property testing.
     */
    static class TestCategory {
        private final int id;
        private final CategoryType categoryType;

        TestCategory(int id, CategoryType categoryType) {
            this.id = id;
            this.categoryType = categoryType;
        }

        public int getId() {
            return id;
        }

        public CategoryType getCategoryType() {
            return categoryType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestCategory that = (TestCategory) o;
            return id == that.id && categoryType == that.categoryType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, categoryType);
        }
    }

    /**
     * Validator class that replicates the required category type validation logic
     * from ProductCategoryService for property testing without database dependencies.
     */
    static class RequiredCategoryValidator {
        private final Map<Integer, Set<TestCategory>> productCategories = new HashMap<>();

        public void assignCategory(int productId, TestCategory category) {
            productCategories.computeIfAbsent(productId, k -> new HashSet<>()).add(category);
        }

        public boolean hasRequiredCategoryType(int productId) {
            Set<TestCategory> categories = productCategories.get(productId);
            if (categories == null || categories.isEmpty()) {
                return false;
            }

            return categories.stream().anyMatch(c -> 
                    c.getCategoryType() == CategoryType.FAN_TYPE || 
                    c.getCategoryType() == CategoryType.ACCESSORY_TYPE);
        }
    }
}
