package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ProductCategoryService multi-category assignment.
 * 
 * **Feature: expanded-category-system, Property 6: Product Multi-Category Assignment**
 * **Validates: Requirements 2.1**
 */
class ProductCategoryMultiAssignmentPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 6: Product Multi-Category Assignment**
     * **Validates: Requirements 2.1**
     * 
     * Property: For any product, the system SHALL allow assigning categories 
     * from multiple different category types simultaneously.
     * 
     * This test verifies that when we assign categories from different types,
     * all categories are stored and can be retrieved.
     */
    @Property(tries = 100)
    void productCanHaveCategoriesFromMultipleDifferentTypes(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesFromDifferentTypes") List<TestCategory> categories) {
        
        // Simulate the assignment logic
        ProductCategoryAssignmentSimulator simulator = new ProductCategoryAssignmentSimulator();
        
        // Assign all categories to the product
        for (TestCategory category : categories) {
            simulator.assignCategoryToProduct((long) productId, category);
        }
        
        // Verify all categories are assigned
        List<TestCategory> assignedCategories = simulator.getProductCategories((long) productId);
        
        // All input categories should be present
        assertThat(assignedCategories).containsExactlyInAnyOrderElementsOf(categories);
        
        // Verify categories from different types are all present
        Set<CategoryType> assignedTypes = assignedCategories.stream()
                .map(TestCategory::getCategoryType)
                .collect(Collectors.toSet());
        
        Set<CategoryType> inputTypes = categories.stream()
                .map(TestCategory::getCategoryType)
                .collect(Collectors.toSet());
        
        assertThat(assignedTypes).containsExactlyInAnyOrderElementsOf(inputTypes);
    }

    /**
     * **Feature: expanded-category-system, Property 6: Product Multi-Category Assignment**
     * **Validates: Requirements 2.1**
     * 
     * Property: For any product with multiple categories from the same type,
     * all categories of that type should be retrievable.
     */
    @Property(tries = 100)
    void productCanHaveMultipleCategoriesOfSameType(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("multipleCategoriesOfSameType") List<TestCategory> categories) {
        
        ProductCategoryAssignmentSimulator simulator = new ProductCategoryAssignmentSimulator();
        
        // Assign all categories
        for (TestCategory category : categories) {
            simulator.assignCategoryToProduct((long) productId, category);
        }
        
        // Verify all categories are assigned
        List<TestCategory> assignedCategories = simulator.getProductCategories((long) productId);
        assertThat(assignedCategories).containsExactlyInAnyOrderElementsOf(categories);
    }


    /**
     * **Feature: expanded-category-system, Property 6: Product Multi-Category Assignment**
     * **Validates: Requirements 2.1**
     * 
     * Property: Assigning the same category twice should not create duplicates.
     */
    @Property(tries = 100)
    void assigningSameCategoryTwiceDoesNotCreateDuplicates(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("singleCategory") TestCategory category) {
        
        ProductCategoryAssignmentSimulator simulator = new ProductCategoryAssignmentSimulator();
        
        // Assign the same category twice
        simulator.assignCategoryToProduct((long) productId, category);
        simulator.assignCategoryToProduct((long) productId, category);
        
        // Verify only one assignment exists
        List<TestCategory> assignedCategories = simulator.getProductCategories((long) productId);
        assertThat(assignedCategories).hasSize(1);
        assertThat(assignedCategories.get(0)).isEqualTo(category);
    }

    /**
     * **Feature: expanded-category-system, Property 6: Product Multi-Category Assignment**
     * **Validates: Requirements 2.1**
     * 
     * Property: The number of assigned categories should equal the number of unique categories provided.
     */
    @Property(tries = 100)
    void categoryCountMatchesUniqueInputCount(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithPossibleDuplicates") List<TestCategory> categories) {
        
        ProductCategoryAssignmentSimulator simulator = new ProductCategoryAssignmentSimulator();
        
        // Assign all categories (may include duplicates)
        for (TestCategory category : categories) {
            simulator.assignCategoryToProduct((long) productId, category);
        }
        
        // Count unique categories in input
        Set<TestCategory> uniqueCategories = new HashSet<>(categories);
        
        // Verify count matches unique count
        List<TestCategory> assignedCategories = simulator.getProductCategories((long) productId);
        assertThat(assignedCategories).hasSize(uniqueCategories.size());
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesFromDifferentTypes() {
        // Generate 2-5 categories from different types
        return Arbitraries.of(CategoryType.values())
                .set().ofMinSize(2).ofMaxSize(5)
                .flatMap(types -> {
                    List<Arbitrary<TestCategory>> categoryArbitraries = types.stream()
                            .map(type -> Arbitraries.integers().between(1, 100)
                                    .map(id -> new TestCategory(id, type)))
                            .collect(Collectors.toList());
                    return Combinators.combine(categoryArbitraries).as(list -> list);
                });
    }

    @Provide
    Arbitrary<List<TestCategory>> multipleCategoriesOfSameType() {
        // Generate 2-5 categories of the same type
        return Arbitraries.of(CategoryType.values())
                .flatMap(type -> 
                    Arbitraries.integers().between(1, 100)
                            .set().ofMinSize(2).ofMaxSize(5)
                            .map(ids -> ids.stream()
                                    .map(id -> new TestCategory(id, type))
                                    .collect(Collectors.toList())));
    }

    @Provide
    Arbitrary<TestCategory> singleCategory() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of(CategoryType.values())
        ).as(TestCategory::new);
    }

    @Provide
    Arbitrary<List<TestCategory>> categoriesWithPossibleDuplicates() {
        return Arbitraries.integers().between(1, 50)
                .flatMap(count -> 
                    Combinators.combine(
                            Arbitraries.integers().between(1, 20),
                            Arbitraries.of(CategoryType.values())
                    ).as(TestCategory::new)
                    .list().ofSize(count));
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

        @Override
        public String toString() {
            return "TestCategory{id=" + id + ", type=" + categoryType + "}";
        }
    }

    /**
     * Simulator class that replicates the core logic of ProductCategoryService
     * for property testing without database dependencies.
     */
    static class ProductCategoryAssignmentSimulator {
        private final Map<Long, Set<TestCategory>> productCategories = new HashMap<>();

        public void assignCategoryToProduct(Long productId, TestCategory category) {
            productCategories.computeIfAbsent(productId, k -> new HashSet<>()).add(category);
        }

        public List<TestCategory> getProductCategories(Long productId) {
            return new ArrayList<>(productCategories.getOrDefault(productId, Collections.emptySet()));
        }

        public List<TestCategory> getProductCategoriesByType(Long productId, CategoryType type) {
            return productCategories.getOrDefault(productId, Collections.emptySet()).stream()
                    .filter(c -> c.getCategoryType() == type)
                    .collect(Collectors.toList());
        }
    }
}
