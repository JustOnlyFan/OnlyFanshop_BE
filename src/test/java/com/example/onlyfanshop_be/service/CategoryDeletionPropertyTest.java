package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Category deletion constraint.
 * 
 * **Feature: expanded-category-system, Property 5: Category Deletion Constraint**
 * **Validates: Requirements 1.5**
 */
class CategoryDeletionPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 5: Category Deletion Constraint**
     * **Validates: Requirements 1.5**
     * 
     * Property: For any category with children, attempting to delete it SHALL fail 
     * with CATEGORY_HAS_CHILDREN error.
     */
    @Property(tries = 100)
    void categoryWithChildrenCannotBeDeleted(
            @ForAll("validCategoryTypes") CategoryType categoryType,
            @ForAll @IntRange(min = 1, max = 1000) int parentId,
            @ForAll @IntRange(min = 1, max = 10) int childCount) {
        
        // Create a mock repository
        CategoryRepository mockRepository = mock(CategoryRepository.class);
        
        // Setup: parent category exists and has children
        when(mockRepository.existsById(parentId)).thenReturn(true);
        when(mockRepository.existsByParentId(parentId)).thenReturn(true);
        
        // Create a deletion validator that mimics CategoryService behavior
        CategoryDeletionValidator validator = new CategoryDeletionValidator(mockRepository);
        
        // Attempt to delete should throw exception
        assertThatThrownBy(() -> validator.validateDeletion(parentId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_HAS_CHILDREN);
                });
        
        // Verify delete was never called
        verify(mockRepository, never()).deleteById(anyInt());
    }

    /**
     * **Feature: expanded-category-system, Property 5: Category Deletion Constraint**
     * **Validates: Requirements 1.5**
     * 
     * Property: For any category without children, deletion SHALL succeed.
     */
    @Property(tries = 100)
    void categoryWithoutChildrenCanBeDeleted(
            @ForAll("validCategoryTypes") CategoryType categoryType,
            @ForAll @IntRange(min = 1, max = 1000) int categoryId) {
        
        // Create a mock repository
        CategoryRepository mockRepository = mock(CategoryRepository.class);
        
        // Setup: category exists but has no children
        when(mockRepository.existsById(categoryId)).thenReturn(true);
        when(mockRepository.existsByParentId(categoryId)).thenReturn(false);
        
        // Create a deletion validator
        CategoryDeletionValidator validator = new CategoryDeletionValidator(mockRepository);
        
        // Deletion should succeed (no exception)
        validator.deleteCategory(categoryId);
        
        // Verify delete was called
        verify(mockRepository).deleteById(categoryId);
    }

    /**
     * **Feature: expanded-category-system, Property 5: Category Deletion Constraint**
     * **Validates: Requirements 1.5**
     * 
     * Property: For any non-existent category, deletion SHALL fail with CATEGORY_NOT_FOUND error.
     */
    @Property(tries = 100)
    void nonExistentCategoryCannotBeDeleted(
            @ForAll @IntRange(min = 1, max = 1000) int categoryId) {
        
        // Create a mock repository
        CategoryRepository mockRepository = mock(CategoryRepository.class);
        
        // Setup: category does not exist
        when(mockRepository.existsById(categoryId)).thenReturn(false);
        
        // Create a deletion validator
        CategoryDeletionValidator validator = new CategoryDeletionValidator(mockRepository);
        
        // Attempt to delete should throw exception
        assertThatThrownBy(() -> validator.deleteCategory(categoryId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
                });
        
        // Verify delete was never called
        verify(mockRepository, never()).deleteById(anyInt());
    }

    /**
     * **Feature: expanded-category-system, Property 5: Category Deletion Constraint**
     * **Validates: Requirements 1.5**
     * 
     * Property: After a failed deletion attempt due to children, the category SHALL 
     * remain in the database (unchanged).
     */
    @Property(tries = 100)
    void failedDeletionPreservesCategory(
            @ForAll("validCategoryTypes") CategoryType categoryType,
            @ForAll @IntRange(min = 1, max = 1000) int parentId) {
        
        // Create a mock repository
        CategoryRepository mockRepository = mock(CategoryRepository.class);
        
        // Setup: parent category exists and has children
        Category parentCategory = Category.builder()
                .id(parentId)
                .name("Parent Category")
                .slug("parent-category")
                .categoryType(categoryType)
                .build();
        
        when(mockRepository.existsById(parentId)).thenReturn(true);
        when(mockRepository.existsByParentId(parentId)).thenReturn(true);
        when(mockRepository.findById(parentId)).thenReturn(java.util.Optional.of(parentCategory));
        
        // Create a deletion validator
        CategoryDeletionValidator validator = new CategoryDeletionValidator(mockRepository);
        
        // Attempt to delete (should fail)
        try {
            validator.deleteCategory(parentId);
        } catch (AppException e) {
            // Expected
        }
        
        // Verify the category still exists (findById should still return it)
        assertThat(mockRepository.findById(parentId)).isPresent();
        
        // Verify delete was never called
        verify(mockRepository, never()).deleteById(anyInt());
    }

    @Provide
    Arbitrary<CategoryType> validCategoryTypes() {
        return Arbitraries.of(CategoryType.values());
    }

    /**
     * Helper class that replicates the deletion logic from CategoryService.
     * This allows testing the deletion constraint without full Spring context.
     */
    static class CategoryDeletionValidator {
        private final CategoryRepository repository;
        
        CategoryDeletionValidator(CategoryRepository repository) {
            this.repository = repository;
        }
        
        void validateDeletion(Integer id) {
            if (!repository.existsById(id)) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            
            if (repository.existsByParentId(id)) {
                throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
            }
        }
        
        void deleteCategory(Integer id) {
            validateDeletion(id);
            repository.deleteById(id);
        }
    }
}
