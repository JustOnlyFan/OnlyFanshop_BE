package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    boolean existsByName(String name);
    
    /**
     * Find all categories by type.
     * @param categoryType the category type to filter by
     * @return list of categories with the specified type
     */
    List<Category> findByCategoryType(CategoryType categoryType);
    
    /**
     * Find all categories by type, ordered by display order.
     * @param categoryType the category type to filter by
     * @return list of categories with the specified type, ordered by displayOrder
     */
    List<Category> findByCategoryTypeOrderByDisplayOrderAsc(CategoryType categoryType);
    
    /**
     * Find child categories by parent ID and type.
     * @param parentId the parent category ID
     * @param categoryType the category type to filter by
     * @return list of child categories
     */
    List<Category> findByParentIdAndCategoryType(Integer parentId, CategoryType categoryType);
    
    /**
     * Find child categories by parent ID and type, ordered by display order.
     * @param parentId the parent category ID
     * @param categoryType the category type to filter by
     * @return list of child categories, ordered by displayOrder
     */
    List<Category> findByParentIdAndCategoryTypeOrderByDisplayOrderAsc(Integer parentId, CategoryType categoryType);
    
    /**
     * Find root categories (no parent) by type.
     * @param categoryType the category type to filter by
     * @return list of root categories with the specified type
     */
    List<Category> findByParentIdIsNullAndCategoryType(CategoryType categoryType);
    
    /**
     * Find root categories (no parent) by type, ordered by display order.
     * @param categoryType the category type to filter by
     * @return list of root categories with the specified type, ordered by displayOrder
     */
    List<Category> findByParentIdIsNullAndCategoryTypeOrderByDisplayOrderAsc(CategoryType categoryType);
    
    /**
     * Find all child categories by parent ID.
     * @param parentId the parent category ID
     * @return list of child categories
     */
    List<Category> findByParentId(Integer parentId);
    
    /**
     * Find all child categories by parent ID, ordered by display order.
     * @param parentId the parent category ID
     * @return list of child categories, ordered by displayOrder
     */
    List<Category> findByParentIdOrderByDisplayOrderAsc(Integer parentId);
    
    /**
     * Check if a category has children.
     * @param parentId the category ID to check
     * @return true if the category has children
     */
    boolean existsByParentId(Integer parentId);
    
    /**
     * Find all active categories by type.
     * @param categoryType the category type to filter by
     * @return list of active categories with the specified type
     */
    List<Category> findByCategoryTypeAndIsActiveTrue(CategoryType categoryType);
    
    /**
     * Find all active root categories by type, ordered by display order.
     * @param categoryType the category type to filter by
     * @return list of active root categories
     */
    List<Category> findByParentIdIsNullAndCategoryTypeAndIsActiveTrueOrderByDisplayOrderAsc(CategoryType categoryType);
    
    /**
     * Custom query to get the full category tree for a specific type.
     * Returns all categories of the type with their hierarchy information.
     * @param categoryType the category type to filter by
     * @return list of categories forming the tree structure
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.categoryType = :categoryType ORDER BY c.parentId NULLS FIRST, c.displayOrder ASC")
    List<Category> findCategoryTreeByType(@Param("categoryType") CategoryType categoryType);
    
    /**
     * Custom query to get root categories with their immediate children eagerly loaded.
     * @param categoryType the category type to filter by
     * @return list of root categories with children loaded
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parentId IS NULL AND c.categoryType = :categoryType ORDER BY c.displayOrder ASC")
    List<Category> findRootCategoriesWithChildren(@Param("categoryType") CategoryType categoryType);
    
    /**
     * Find category by slug.
     * @param slug the category slug
     * @return the category with the specified slug, or null if not found
     */
    Category findBySlug(String slug);
    
    /**
     * Check if a category with the given slug exists.
     * @param slug the slug to check
     * @return true if a category with the slug exists
     */
    boolean existsBySlug(String slug);
    
    /**
     * Count categories by type.
     * @param categoryType the category type to count
     * @return the number of categories with the specified type
     */
    long countByCategoryType(CategoryType categoryType);
}

