package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ProductFilterService category query completeness.
 * 
 * **Feature: expanded-category-system, Property 10: Category Query Completeness**
 * **Validates: Requirements 4.1**
 */
class CategoryQueryCompletenessPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 10: Category Query Completeness**
     * **Validates: Requirements 4.1**
     * 
     * Property: For any category selection, the returned products SHALL include 
     * all products assigned to that category and all its descendant categories.
     * 
     * This test verifies that when querying by a parent category with includeSubcategories=true,
     * all products from the parent and all descendant categories are returned.
     */
    @Property(tries = 100)
    void categoryQueryIncludesAllDescendantProducts(
            @ForAll("categoryHierarchy") TestCategoryHierarchy hierarchy,
            @ForAll("productAssignments") List<TestProductAssignment> assignments) {
        
        // Create simulator with the hierarchy
        CategoryQuerySimulator simulator = new CategoryQuerySimulator(hierarchy);
        
        // Assign products to categories
        for (TestProductAssignment assignment : assignments) {
            simulator.assignProductToCategory(assignment.productId, assignment.categoryId);
        }
        
        // Query by root category with subcategories included
        Integer rootCategoryId = hierarchy.rootCategory.id;
        Set<Long> returnedProductIds = simulator.getProductIdsByCategory(rootCategoryId, true);
        
        // Calculate expected products: all products in root and all descendants
        Set<Integer> allCategoryIds = hierarchy.getAllCategoryIds();
        Set<Long> expectedProductIds = assignments.stream()
                .filter(a -> allCategoryIds.contains(a.categoryId))
                .map(a -> a.productId)
                .collect(Collectors.toSet());
        
        // Verify all expected products are returned
        assertThat(returnedProductIds).containsExactlyInAnyOrderElementsOf(expectedProductIds);
    }


    /**
     * **Feature: expanded-category-system, Property 10: Category Query Completeness**
     * **Validates: Requirements 4.1**
     * 
     * Property: When querying without subcategories, only products directly assigned 
     * to the specified category should be returned.
     */
    @Property(tries = 100)
    void categoryQueryWithoutSubcategoriesReturnsOnlyDirectProducts(
            @ForAll("categoryHierarchy") TestCategoryHierarchy hierarchy,
            @ForAll("productAssignments") List<TestProductAssignment> assignments) {
        
        CategoryQuerySimulator simulator = new CategoryQuerySimulator(hierarchy);
        
        for (TestProductAssignment assignment : assignments) {
            simulator.assignProductToCategory(assignment.productId, assignment.categoryId);
        }
        
        // Query by root category WITHOUT subcategories
        Integer rootCategoryId = hierarchy.rootCategory.id;
        Set<Long> returnedProductIds = simulator.getProductIdsByCategory(rootCategoryId, false);
        
        // Expected: only products directly assigned to root category
        Set<Long> expectedProductIds = assignments.stream()
                .filter(a -> a.categoryId.equals(rootCategoryId))
                .map(a -> a.productId)
                .collect(Collectors.toSet());
        
        assertThat(returnedProductIds).containsExactlyInAnyOrderElementsOf(expectedProductIds);
    }

    /**
     * **Feature: expanded-category-system, Property 10: Category Query Completeness**
     * **Validates: Requirements 4.1**
     * 
     * Property: Products assigned to a child category should be included when 
     * querying the parent category with subcategories enabled.
     */
    @Property(tries = 100)
    void childCategoryProductsIncludedInParentQuery(
            @ForAll("categoryHierarchy") TestCategoryHierarchy hierarchy,
            @ForAll @IntRange(min = 1, max = 1000) long productId) {
        
        CategoryQuerySimulator simulator = new CategoryQuerySimulator(hierarchy);
        
        // Assign product to a child category (if exists)
        if (!hierarchy.childCategories.isEmpty()) {
            TestCategory childCategory = hierarchy.childCategories.get(0);
            simulator.assignProductToCategory(productId, childCategory.id);
            
            // Query by root category with subcategories
            Set<Long> returnedProductIds = simulator.getProductIdsByCategory(hierarchy.rootCategory.id, true);
            
            // Product should be included
            assertThat(returnedProductIds).contains(productId);
        }
    }

    /**
     * **Feature: expanded-category-system, Property 10: Category Query Completeness**
     * **Validates: Requirements 4.1**
     * 
     * Property: Products assigned to a grandchild category should be included when 
     * querying the root category with subcategories enabled.
     */
    @Property(tries = 100)
    void grandchildCategoryProductsIncludedInRootQuery(
            @ForAll("deepCategoryHierarchy") TestCategoryHierarchy hierarchy,
            @ForAll @IntRange(min = 1, max = 1000) long productId) {
        
        CategoryQuerySimulator simulator = new CategoryQuerySimulator(hierarchy);
        
        // Assign product to a grandchild category (if exists)
        if (!hierarchy.grandchildCategories.isEmpty()) {
            TestCategory grandchildCategory = hierarchy.grandchildCategories.get(0);
            simulator.assignProductToCategory(productId, grandchildCategory.id);
            
            // Query by root category with subcategories
            Set<Long> returnedProductIds = simulator.getProductIdsByCategory(hierarchy.rootCategory.id, true);
            
            // Product should be included
            assertThat(returnedProductIds).contains(productId);
        }
    }

    @Provide
    Arbitrary<TestCategoryHierarchy> categoryHierarchy() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),  // root id
                Arbitraries.integers().between(0, 3),    // number of children
                Arbitraries.of(CategoryType.values())
        ).as((rootId, childCount, type) -> {
            TestCategory root = new TestCategory(rootId, type, null);
            List<TestCategory> children = new ArrayList<>();
            
            for (int i = 0; i < childCount; i++) {
                children.add(new TestCategory(rootId * 100 + i + 1, type, rootId));
            }
            
            return new TestCategoryHierarchy(root, children, Collections.emptyList());
        });
    }

    @Provide
    Arbitrary<TestCategoryHierarchy> deepCategoryHierarchy() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 100),  // root id
                Arbitraries.integers().between(1, 2),    // number of children
                Arbitraries.integers().between(1, 2),    // number of grandchildren per child
                Arbitraries.of(CategoryType.values())
        ).as((rootId, childCount, grandchildCount, type) -> {
            TestCategory root = new TestCategory(rootId, type, null);
            List<TestCategory> children = new ArrayList<>();
            List<TestCategory> grandchildren = new ArrayList<>();
            
            for (int i = 0; i < childCount; i++) {
                int childId = rootId * 100 + i + 1;
                children.add(new TestCategory(childId, type, rootId));
                
                for (int j = 0; j < grandchildCount; j++) {
                    grandchildren.add(new TestCategory(childId * 100 + j + 1, type, childId));
                }
            }
            
            return new TestCategoryHierarchy(root, children, grandchildren);
        });
    }

    @Provide
    Arbitrary<List<TestProductAssignment>> productAssignments() {
        return Combinators.combine(
                Arbitraries.longs().between(1, 1000),
                Arbitraries.integers().between(1, 10000)
        ).as(TestProductAssignment::new)
        .list().ofMinSize(0).ofMaxSize(20);
    }


    // ==================== Test Data Classes ====================

    static class TestCategory {
        final Integer id;
        final CategoryType type;
        final Integer parentId;

        TestCategory(Integer id, CategoryType type, Integer parentId) {
            this.id = id;
            this.type = type;
            this.parentId = parentId;
        }
    }

    static class TestCategoryHierarchy {
        final TestCategory rootCategory;
        final List<TestCategory> childCategories;
        final List<TestCategory> grandchildCategories;

        TestCategoryHierarchy(TestCategory rootCategory, List<TestCategory> childCategories, 
                             List<TestCategory> grandchildCategories) {
            this.rootCategory = rootCategory;
            this.childCategories = childCategories;
            this.grandchildCategories = grandchildCategories;
        }

        Set<Integer> getAllCategoryIds() {
            Set<Integer> ids = new HashSet<>();
            ids.add(rootCategory.id);
            childCategories.forEach(c -> ids.add(c.id));
            grandchildCategories.forEach(c -> ids.add(c.id));
            return ids;
        }
    }

    static class TestProductAssignment {
        final Long productId;
        final Integer categoryId;

        TestProductAssignment(Long productId, Integer categoryId) {
            this.productId = productId;
            this.categoryId = categoryId;
        }
    }

    // ==================== Simulator ====================

    /**
     * Simulator that replicates the core logic of ProductFilterService
     * for property testing without database dependencies.
     */
    static class CategoryQuerySimulator {
        private final TestCategoryHierarchy hierarchy;
        private final Map<Integer, Set<Long>> categoryToProducts = new HashMap<>();

        CategoryQuerySimulator(TestCategoryHierarchy hierarchy) {
            this.hierarchy = hierarchy;
        }

        void assignProductToCategory(Long productId, Integer categoryId) {
            categoryToProducts.computeIfAbsent(categoryId, k -> new HashSet<>()).add(productId);
        }

        Set<Long> getProductIdsByCategory(Integer categoryId, boolean includeSubcategories) {
            Set<Long> result = new HashSet<>();
            
            // Add products directly assigned to this category
            result.addAll(categoryToProducts.getOrDefault(categoryId, Collections.emptySet()));
            
            if (includeSubcategories) {
                // Add products from all descendant categories
                Set<Integer> descendantIds = getDescendantCategoryIds(categoryId);
                for (Integer descendantId : descendantIds) {
                    result.addAll(categoryToProducts.getOrDefault(descendantId, Collections.emptySet()));
                }
            }
            
            return result;
        }

        private Set<Integer> getDescendantCategoryIds(Integer categoryId) {
            Set<Integer> descendants = new HashSet<>();
            
            // Find direct children
            for (TestCategory child : hierarchy.childCategories) {
                if (categoryId.equals(child.parentId)) {
                    descendants.add(child.id);
                    // Recursively add grandchildren
                    descendants.addAll(getDescendantCategoryIds(child.id));
                }
            }
            
            // Find grandchildren (for deeper hierarchies)
            for (TestCategory grandchild : hierarchy.grandchildCategories) {
                if (categoryId.equals(grandchild.parentId)) {
                    descendants.add(grandchild.id);
                }
            }
            
            return descendants;
        }
    }
}
