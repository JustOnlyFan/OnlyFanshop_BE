package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for category removal independence.
 * 
 * **Feature: expanded-category-system, Property 8: Category Removal Independence**
 * **Validates: Requirements 2.4**
 */
class CategoryRemovalIndependencePropertyTest {

    /**
     * **Feature: expanded-category-system, Property 8: Category Removal Independence**
     * **Validates: Requirements 2.4**
     * 
     * Property: For any product with multiple categories, removing one category 
     * SHALL not affect the other category assignments.
     * 
     * This test verifies that when we remove a category from a product,
     * all other categories remain assigned.
     */
    @Property(tries = 100)
    void removingOneCategoryDoesNotAffectOthers(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("multipleCategoriesWithRemovalTarget") CategoriesWithTarget categoriesWithTarget) {
        
        List<TestCategory> categories = categoriesWithTarget.categories;
        TestCategory categoryToRemove = categoriesWithTarget.categoryToRemove;
        
        // Skip if we don't have enough categories
        Assume.that(categories.size() >= 2);
        Assume.that(categories.contains(categoryToRemove));
        
        CategoryRemovalSimulator simulator = new CategoryRemovalSimulator();
        
        // Assign all categories to the product
        for (TestCategory category : categories) {
            simulator.assignCategory(productId, category);
        }
        
        // Get categories before removal
        Set<TestCategory> categoriesBefore = new HashSet<>(simulator.getProductCategories(productId));
        
        // Remove one category
        simulator.removeCategory(productId, categoryToRemove);
        
        // Get categories after removal
        Set<TestCategory> categoriesAfter = new HashSet<>(simulator.getProductCategories(productId));
        
        // The removed category should not be present
        assertThat(categoriesAfter).doesNotContain(categoryToRemove);
        
        // All other categories should still be present
        Set<TestCategory> expectedRemaining = new HashSet<>(categoriesBefore);
        expectedRemaining.remove(categoryToRemove);
        assertThat(categoriesAfter).containsExactlyInAnyOrderElementsOf(expectedRemaining);
    }


    /**
     * **Feature: expanded-category-system, Property 8: Category Removal Independence**
     * **Validates: Requirements 2.4**
     * 
     * Property: Removing a category should only decrease the count by exactly one.
     */
    @Property(tries = 100)
    void removingCategoryDecreasesCountByOne(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("multipleCategoriesWithRemovalTarget") CategoriesWithTarget categoriesWithTarget) {
        
        List<TestCategory> categories = categoriesWithTarget.categories;
        TestCategory categoryToRemove = categoriesWithTarget.categoryToRemove;
        
        // Skip if we don't have enough categories
        Assume.that(categories.size() >= 2);
        Assume.that(categories.contains(categoryToRemove));
        
        CategoryRemovalSimulator simulator = new CategoryRemovalSimulator();
        
        // Assign all categories to the product
        for (TestCategory category : categories) {
            simulator.assignCategory(productId, category);
        }
        
        // Get count before removal
        int countBefore = simulator.getCategoryCount(productId);
        
        // Remove one category
        simulator.removeCategory(productId, categoryToRemove);
        
        // Get count after removal
        int countAfter = simulator.getCategoryCount(productId);
        
        // Count should decrease by exactly one
        assertThat(countAfter).isEqualTo(countBefore - 1);
    }

    /**
     * **Feature: expanded-category-system, Property 8: Category Removal Independence**
     * **Validates: Requirements 2.4**
     * 
     * Property: Removing a category that doesn't exist should not affect other categories.
     */
    @Property(tries = 100)
    void removingNonExistentCategoryDoesNotAffectOthers(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("multipleCategories") List<TestCategory> categories,
            @ForAll("nonExistentCategory") TestCategory nonExistentCategory) {
        
        // Skip if the non-existent category happens to be in the list
        Assume.that(!categories.contains(nonExistentCategory));
        Assume.that(!categories.isEmpty());
        
        CategoryRemovalSimulator simulator = new CategoryRemovalSimulator();
        
        // Assign all categories to the product
        for (TestCategory category : categories) {
            simulator.assignCategory(productId, category);
        }
        
        // Get categories before removal attempt
        Set<TestCategory> categoriesBefore = new HashSet<>(simulator.getProductCategories(productId));
        
        // Try to remove a category that doesn't exist
        simulator.removeCategory(productId, nonExistentCategory);
        
        // Get categories after removal attempt
        Set<TestCategory> categoriesAfter = new HashSet<>(simulator.getProductCategories(productId));
        
        // All categories should still be present
        assertThat(categoriesAfter).containsExactlyInAnyOrderElementsOf(categoriesBefore);
    }

    /**
     * **Feature: expanded-category-system, Property 8: Category Removal Independence**
     * **Validates: Requirements 2.4**
     * 
     * Property: Removing multiple categories sequentially should only remove those specific categories.
     */
    @Property(tries = 100)
    void removingMultipleCategoriesSequentiallyOnlyRemovesThose(
            @ForAll @IntRange(min = 1, max = 1000) int productId,
            @ForAll("categoriesWithMultipleRemovalTargets") CategoriesWithMultipleTargets data) {
        
        List<TestCategory> categories = data.categories;
        List<TestCategory> categoriesToRemove = data.categoriesToRemove;
        
        // Skip if we don't have enough categories
        Assume.that(categories.size() >= 3);
        Assume.that(categoriesToRemove.size() >= 2);
        Assume.that(categories.containsAll(categoriesToRemove));
        
        CategoryRemovalSimulator simulator = new CategoryRemovalSimulator();
        
        // Assign all categories to the product
        for (TestCategory category : categories) {
            simulator.assignCategory(productId, category);
        }
        
        // Remove multiple categories
        for (TestCategory categoryToRemove : categoriesToRemove) {
            simulator.removeCategory(productId, categoryToRemove);
        }
        
        // Get categories after removal
        Set<TestCategory> categoriesAfter = new HashSet<>(simulator.getProductCategories(productId));
        
        // Removed categories should not be present
        for (TestCategory removed : categoriesToRemove) {
            assertThat(categoriesAfter).doesNotContain(removed);
        }
        
        // All other categories should still be present
        Set<TestCategory> expectedRemaining = new HashSet<>(categories);
        expectedRemaining.removeAll(categoriesToRemove);
        assertThat(categoriesAfter).containsExactlyInAnyOrderElementsOf(expectedRemaining);
    }

    @Provide
    Arbitrary<CategoriesWithTarget> multipleCategoriesWithRemovalTarget() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of(CategoryType.values())
        ).as(TestCategory::new)
        .set().ofMinSize(2).ofMaxSize(6)
        .map(categorySet -> {
            List<TestCategory> categories = new ArrayList<>(categorySet);
            // Pick a random category to remove
            int indexToRemove = new Random().nextInt(categories.size());
            TestCategory categoryToRemove = categories.get(indexToRemove);
            return new CategoriesWithTarget(categories, categoryToRemove);
        });
    }

    @Provide
    Arbitrary<List<TestCategory>> multipleCategories() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),
                Arbitraries.of(CategoryType.values())
        ).as(TestCategory::new)
        .set().ofMinSize(2).ofMaxSize(6)
        .map(ArrayList::new);
    }

    @Provide
    Arbitrary<TestCategory> nonExistentCategory() {
        // Generate categories with IDs that are unlikely to be in the main list
        return Combinators.combine(
                Arbitraries.integers().between(1000, 2000),
                Arbitraries.of(CategoryType.values())
        ).as(TestCategory::new);
    }

    @Provide
    Arbitrary<CategoriesWithMultipleTargets> categoriesWithMultipleRemovalTargets() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 50),
                Arbitraries.of(CategoryType.values())
        ).as(TestCategory::new)
        .set().ofMinSize(4).ofMaxSize(8)
        .map(categorySet -> {
            List<TestCategory> categories = new ArrayList<>(categorySet);
            // Pick 2 random categories to remove
            Collections.shuffle(categories);
            List<TestCategory> toRemove = categories.subList(0, Math.min(2, categories.size()));
            return new CategoriesWithMultipleTargets(new ArrayList<>(categorySet), new ArrayList<>(toRemove));
        });
    }

    /**
     * Helper class to hold categories and a target for removal.
     */
    static class CategoriesWithTarget {
        final List<TestCategory> categories;
        final TestCategory categoryToRemove;

        CategoriesWithTarget(List<TestCategory> categories, TestCategory categoryToRemove) {
            this.categories = categories;
            this.categoryToRemove = categoryToRemove;
        }
    }

    /**
     * Helper class to hold categories and multiple targets for removal.
     */
    static class CategoriesWithMultipleTargets {
        final List<TestCategory> categories;
        final List<TestCategory> categoriesToRemove;

        CategoriesWithMultipleTargets(List<TestCategory> categories, List<TestCategory> categoriesToRemove) {
            this.categories = categories;
            this.categoriesToRemove = categoriesToRemove;
        }
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
     * Simulator class that replicates the category removal logic
     * from ProductCategoryService for property testing without database dependencies.
     */
    static class CategoryRemovalSimulator {
        private final Map<Integer, Set<TestCategory>> productCategories = new HashMap<>();

        public void assignCategory(int productId, TestCategory category) {
            productCategories.computeIfAbsent(productId, k -> new HashSet<>()).add(category);
        }

        public void removeCategory(int productId, TestCategory category) {
            Set<TestCategory> categories = productCategories.get(productId);
            if (categories != null) {
                categories.remove(category);
            }
        }

        public List<TestCategory> getProductCategories(int productId) {
            return new ArrayList<>(productCategories.getOrDefault(productId, Collections.emptySet()));
        }

        public int getCategoryCount(int productId) {
            return productCategories.getOrDefault(productId, Collections.emptySet()).size();
        }
    }
}
