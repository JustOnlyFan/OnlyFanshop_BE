package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Category hierarchy validation.
 * 
 * **Feature: expanded-category-system, Property 2: Parent-Child Category Type Consistency**
 * **Feature: expanded-category-system, Property 3: Category Hierarchy Depth Limit**
 * **Validates: Requirements 1.2, 1.3**
 */
class CategoryHierarchyPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 2: Parent-Child Category Type Consistency**
     * **Validates: Requirements 1.2**
     * 
     * Property: For any sub-category creation, the parent category's type SHALL equal 
     * the child category's type.
     */
    @Property(tries = 100)
    void parentChildCategoryTypeMustMatch(
            @ForAll("validCategoryTypes") CategoryType parentType,
            @ForAll("validCategoryTypes") CategoryType childType) {
        
        Category parent = Category.builder()
                .id(1)
                .name("Parent Category")
                .slug("parent-category")
                .categoryType(parentType)
                .build();
        
        boolean isValidChild = parent.isValidChildType(childType);
        
        // Child type is valid only if it matches parent type
        assertThat(isValidChild).isEqualTo(parentType == childType);
    }

    /**
     * **Feature: expanded-category-system, Property 2: Parent-Child Category Type Consistency**
     * **Validates: Requirements 1.2**
     * 
     * Property: For any category type, a parent and child of the same type SHALL be valid.
     */
    @Property(tries = 100)
    void sameTypeCategoriesAreValidParentChild(@ForAll("validCategoryTypes") CategoryType categoryType) {
        Category parent = Category.builder()
                .id(1)
                .name("Parent")
                .slug("parent")
                .categoryType(categoryType)
                .build();
        
        assertThat(parent.isValidChildType(categoryType)).isTrue();
    }

    /**
     * **Feature: expanded-category-system, Property 3: Category Hierarchy Depth Limit**
     * **Validates: Requirements 1.3**
     * 
     * Property: For any category in the system, the depth from root to that category 
     * SHALL be at most 3 levels.
     */
    @Property(tries = 100)
    void categoryHierarchyDepthIsAtMostThree(@ForAll("validCategoryTypes") CategoryType categoryType) {
        // Create a 3-level hierarchy
        Category root = Category.builder()
                .id(1)
                .name("Root")
                .slug("root")
                .categoryType(categoryType)
                .build();
        
        Category level2 = Category.builder()
                .id(2)
                .name("Level 2")
                .slug("level-2")
                .categoryType(categoryType)
                .parentId(1)
                .parent(root)
                .build();
        
        Category level3 = Category.builder()
                .id(3)
                .name("Level 3")
                .slug("level-3")
                .categoryType(categoryType)
                .parentId(2)
                .parent(level2)
                .build();
        
        // Verify depths
        assertThat(root.getDepth()).isEqualTo(1);
        assertThat(level2.getDepth()).isEqualTo(2);
        assertThat(level3.getDepth()).isEqualTo(3);
        
        // Root and level 2 can have children, level 3 cannot
        assertThat(root.canHaveChildren()).isTrue();
        assertThat(level2.canHaveChildren()).isTrue();
        assertThat(level3.canHaveChildren()).isFalse();
    }

    /**
     * **Feature: expanded-category-system, Property 3: Category Hierarchy Depth Limit**
     * **Validates: Requirements 1.3**
     * 
     * Property: A root category (no parent) SHALL have depth 1.
     */
    @Property(tries = 100)
    void rootCategoryHasDepthOne(
            @ForAll("validCategoryTypes") CategoryType categoryType,
            @ForAll @IntRange(min = 1, max = 1000) int categoryId) {
        
        Category root = Category.builder()
                .id(categoryId)
                .name("Root Category " + categoryId)
                .slug("root-category-" + categoryId)
                .categoryType(categoryType)
                .parent(null)
                .parentId(null)
                .build();
        
        assertThat(root.getDepth()).isEqualTo(1);
        assertThat(root.canHaveChildren()).isTrue();
    }

    /**
     * **Feature: expanded-category-system, Property 3: Category Hierarchy Depth Limit**
     * **Validates: Requirements 1.3**
     * 
     * Property: For any category at depth N, its child SHALL have depth N+1.
     */
    @Property(tries = 100)
    void childDepthIsParentDepthPlusOne(@ForAll("validCategoryTypes") CategoryType categoryType) {
        Category parent = Category.builder()
                .id(1)
                .name("Parent")
                .slug("parent")
                .categoryType(categoryType)
                .build();
        
        Category child = Category.builder()
                .id(2)
                .name("Child")
                .slug("child")
                .categoryType(categoryType)
                .parentId(1)
                .parent(parent)
                .build();
        
        assertThat(child.getDepth()).isEqualTo(parent.getDepth() + 1);
    }

    @Provide
    Arbitrary<CategoryType> validCategoryTypes() {
        return Arbitraries.of(CategoryType.values());
    }
}
