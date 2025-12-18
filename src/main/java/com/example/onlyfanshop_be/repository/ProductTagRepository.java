package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ProductTag entities.
 * Handles the many-to-many relationship between products and tags with validity periods.
 */
@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    
    /**
     * Find all product-tag relationships for a product.
     * @param productId the product ID
     * @return list of product-tag relationships
     */
    List<ProductTag> findByProductId(Long productId);
    
    /**
     * Find all product-tag relationships for a product with tag eagerly loaded.
     * @param productId the product ID
     * @return list of product-tag relationships with tags
     */
    @Query("SELECT pt FROM ProductTag pt JOIN FETCH pt.tag WHERE pt.productId = :productId")
    List<ProductTag> findByProductIdWithTag(@Param("productId") Long productId);
    
    /**
     * Find active tags for a product at the current time.
     * A tag is active if:
     * - validFrom is null OR validFrom <= current time
     * - validUntil is null OR validUntil >= current time
     * @param productId the product ID
     * @return list of active product-tag relationships
     */
    @Query("SELECT pt FROM ProductTag pt WHERE pt.productId = :productId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<ProductTag> findActiveTagsByProductId(@Param("productId") Long productId);
    
    /**
     * Find active tags for a product at the current time with tag eagerly loaded.
     * @param productId the product ID
     * @return list of active product-tag relationships with tags
     */
    @Query("SELECT pt FROM ProductTag pt JOIN FETCH pt.tag WHERE pt.productId = :productId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<ProductTag> findActiveTagsByProductIdWithTag(@Param("productId") Long productId);
    
    /**
     * Find active tags for a product at a specific time.
     * @param productId the product ID
     * @param dateTime the date/time to check
     * @return list of active product-tag relationships at the specified time
     */
    @Query("SELECT pt FROM ProductTag pt WHERE pt.productId = :productId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= :dateTime) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= :dateTime)")
    List<ProductTag> findActiveTagsByProductIdAtTime(@Param("productId") Long productId, @Param("dateTime") LocalDateTime dateTime);
    
    /**
     * Check if a specific product-tag relationship exists.
     * @param productId the product ID
     * @param tagId the tag ID
     * @return true if the relationship exists
     */
    boolean existsByProductIdAndTagId(Long productId, Integer tagId);
    
    /**
     * Find a specific product-tag relationship.
     * @param productId the product ID
     * @param tagId the tag ID
     * @return the product-tag relationship if found
     */
    Optional<ProductTag> findByProductIdAndTagId(Long productId, Integer tagId);
    
    /**
     * Delete all product-tag relationships for a product.
     * @param productId the product ID
     */
    void deleteByProductId(Long productId);
    
    /**
     * Delete a specific product-tag relationship.
     * @param productId the product ID
     * @param tagId the tag ID
     */
    void deleteByProductIdAndTagId(Long productId, Integer tagId);
    
    /**
     * Find all products with a specific tag.
     * @param tagId the tag ID
     * @return list of product-tag relationships
     */
    List<ProductTag> findByTagId(Integer tagId);
    
    /**
     * Find all products with a specific tag that is currently active.
     * @param tagId the tag ID
     * @return list of active product-tag relationships
     */
    @Query("SELECT pt FROM ProductTag pt WHERE pt.tagId = :tagId " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<ProductTag> findActiveProductsByTagId(@Param("tagId") Integer tagId);
    
    /**
     * Count the number of tags assigned to a product.
     * @param productId the product ID
     * @return the number of tags
     */
    long countByProductId(Long productId);
    
    /**
     * Count the number of products with a specific tag.
     * @param tagId the tag ID
     * @return the number of products
     */
    long countByTagId(Integer tagId);
    
    /**
     * Find all product IDs that have a specific tag code (currently active).
     * @param tagCode the tag code
     * @return list of product IDs
     */
    @Query("SELECT pt.productId FROM ProductTag pt JOIN pt.tag t WHERE t.code = :tagCode " +
           "AND (pt.validFrom IS NULL OR pt.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (pt.validUntil IS NULL OR pt.validUntil >= CURRENT_TIMESTAMP)")
    List<Long> findProductIdsByActiveTagCode(@Param("tagCode") String tagCode);
}
