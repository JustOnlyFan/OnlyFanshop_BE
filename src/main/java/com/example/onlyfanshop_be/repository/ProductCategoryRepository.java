package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ProductCategory entities.
 * Handles the many-to-many relationship between products and categories.
 */
@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    
    /**
     * Find all product-category relationships for a product.
     * @param productId the product ID
     * @return list of product-category relationships
     */
    List<ProductCategory> findByProductId(Long productId);
    
    /**
     * Find all product-category relationships for a product with category eagerly loaded.
     * @param productId the product ID
     * @return list of product-category relationships with categories
     */
    @Query("SELECT pc FROM ProductCategory pc JOIN FETCH pc.category WHERE pc.productId = :productId")
    List<ProductCategory> findByProductIdWithCategory(@Param("productId") Long productId);
    
    /**
     * Find product-category relationships for a product filtered by category type.
     * @param productId the product ID
     * @param categoryType the category type to filter by
     * @return list of product-category relationships matching the criteria
     */
    @Query("SELECT pc FROM ProductCategory pc JOIN pc.category c WHERE pc.productId = :productId AND c.categoryType = :categoryType")
    List<ProductCategory> findByProductIdAndCategoryType(@Param("productId") Long productId, @Param("categoryType") CategoryType categoryType);
    
    /**
     * Find product-category relationships for a product filtered by category type with category eagerly loaded.
     * @param productId the product ID
     * @param categoryType the category type to filter by
     * @return list of product-category relationships with categories
     */
    @Query("SELECT pc FROM ProductCategory pc JOIN FETCH pc.category c WHERE pc.productId = :productId AND c.categoryType = :categoryType")
    List<ProductCategory> findByProductIdAndCategoryTypeWithCategory(@Param("productId") Long productId, @Param("categoryType") CategoryType categoryType);
    
    /**
     * Check if a product has at least one category of the specified type.
     * @param productId the product ID
     * @param categoryType the category type to check
     * @return true if the product has at least one category of the specified type
     */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM ProductCategory pc JOIN pc.category c WHERE pc.productId = :productId AND c.categoryType = :categoryType")
    boolean existsByProductIdAndCategoryType(@Param("productId") Long productId, @Param("categoryType") CategoryType categoryType);
    
    /**
     * Check if a specific product-category relationship exists.
     * @param productId the product ID
     * @param categoryId the category ID
     * @return true if the relationship exists
     */
    boolean existsByProductIdAndCategoryId(Long productId, Integer categoryId);
    
    /**
     * Find a specific product-category relationship.
     * @param productId the product ID
     * @param categoryId the category ID
     * @return the product-category relationship if found
     */
    Optional<ProductCategory> findByProductIdAndCategoryId(Long productId, Integer categoryId);
    
    /**
     * Delete all product-category relationships for a product.
     * @param productId the product ID
     */
    void deleteByProductId(Long productId);
    
    /**
     * Delete a specific product-category relationship.
     * @param productId the product ID
     * @param categoryId the category ID
     */
    void deleteByProductIdAndCategoryId(Long productId, Integer categoryId);
    
    /**
     * Find all products assigned to a specific category.
     * @param categoryId the category ID
     * @return list of product-category relationships
     */
    List<ProductCategory> findByCategoryId(Integer categoryId);
    
    /**
     * Find the primary category relationship for a product.
     * @param productId the product ID
     * @return the primary product-category relationship if found
     */
    Optional<ProductCategory> findByProductIdAndIsPrimaryTrue(Long productId);
    
    /**
     * Count the number of categories assigned to a product.
     * @param productId the product ID
     * @return the number of categories
     */
    long countByProductId(Long productId);
    
    /**
     * Count the number of products assigned to a category.
     * @param categoryId the category ID
     * @return the number of products
     */
    long countByCategoryId(Integer categoryId);
    
    /**
     * Find all product IDs that have at least one category of the specified type.
     * @param categoryType the category type
     * @return list of product IDs
     */
    @Query("SELECT DISTINCT pc.productId FROM ProductCategory pc JOIN pc.category c WHERE c.categoryType = :categoryType")
    List<Long> findProductIdsByCategoryType(@Param("categoryType") CategoryType categoryType);
    
    /**
     * Find all product IDs assigned to a specific category or its descendants.
     * @param categoryIds list of category IDs (including parent and all descendants)
     * @return list of product IDs
     */
    @Query("SELECT DISTINCT pc.productId FROM ProductCategory pc WHERE pc.categoryId IN :categoryIds")
    List<Long> findProductIdsByCategoryIds(@Param("categoryIds") List<Integer> categoryIds);
}
