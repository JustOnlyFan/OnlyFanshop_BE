package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing AccessoryCompatibility entities.
 * Handles the relationship between accessory products and compatible fan types/brands/models.
 */
@Repository
public interface AccessoryCompatibilityRepository extends JpaRepository<AccessoryCompatibility, Long> {
    
    /**
     * Find all compatibility entries for an accessory product.
     * @param accessoryProductId the accessory product ID
     * @return list of compatibility entries
     */
    List<AccessoryCompatibility> findByAccessoryProductId(Long accessoryProductId);
    
    /**
     * Find all compatibility entries for an accessory product with related entities eagerly loaded.
     * @param accessoryProductId the accessory product ID
     * @return list of compatibility entries with fan type and brand loaded
     */
    @Query("SELECT ac FROM AccessoryCompatibility ac " +
           "LEFT JOIN FETCH ac.compatibleFanType " +
           "LEFT JOIN FETCH ac.compatibleBrand " +
           "WHERE ac.accessoryProductId = :accessoryProductId")
    List<AccessoryCompatibility> findByAccessoryProductIdWithDetails(@Param("accessoryProductId") Long accessoryProductId);
    
    /**
     * Find all compatibility entries for a specific fan type.
     * @param compatibleFanTypeId the fan type category ID
     * @return list of compatibility entries
     */
    List<AccessoryCompatibility> findByCompatibleFanTypeId(Integer compatibleFanTypeId);
    
    /**
     * Find all accessory product IDs compatible with a specific fan type.
     * @param compatibleFanTypeId the fan type category ID
     * @return list of accessory product IDs
     */
    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac WHERE ac.compatibleFanTypeId = :fanTypeId")
    List<Long> findAccessoryProductIdsByCompatibleFanTypeId(@Param("fanTypeId") Integer compatibleFanTypeId);
    
    /**
     * Find all compatibility entries for a specific brand.
     * @param compatibleBrandId the brand ID
     * @return list of compatibility entries
     */
    List<AccessoryCompatibility> findByCompatibleBrandId(Integer compatibleBrandId);
    
    /**
     * Find all accessory product IDs compatible with a specific brand.
     * @param compatibleBrandId the brand ID
     * @return list of accessory product IDs
     */
    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac WHERE ac.compatibleBrandId = :brandId")
    List<Long> findAccessoryProductIdsByCompatibleBrandId(@Param("brandId") Integer compatibleBrandId);
    
    /**
     * Find compatibility entries matching a specific model pattern.
     * @param modelPattern the model pattern to search for (supports LIKE syntax)
     * @return list of compatibility entries
     */
    @Query("SELECT ac FROM AccessoryCompatibility ac WHERE ac.compatibleModel LIKE %:modelPattern%")
    List<AccessoryCompatibility> findByCompatibleModelContaining(@Param("modelPattern") String modelPattern);
    
    /**
     * Find all accessory product IDs compatible with a specific fan type and brand.
     * @param fanTypeId the fan type category ID
     * @param brandId the brand ID
     * @return list of accessory product IDs
     */
    @Query("SELECT DISTINCT ac.accessoryProductId FROM AccessoryCompatibility ac " +
           "WHERE ac.compatibleFanTypeId = :fanTypeId AND ac.compatibleBrandId = :brandId")
    List<Long> findAccessoryProductIdsByFanTypeAndBrand(@Param("fanTypeId") Integer fanTypeId, @Param("brandId") Integer brandId);
    
    /**
     * Check if an accessory product has any compatibility entries.
     * @param accessoryProductId the accessory product ID
     * @return true if the accessory has compatibility entries
     */
    boolean existsByAccessoryProductId(Long accessoryProductId);
    
    /**
     * Check if an accessory is compatible with a specific fan type.
     * @param accessoryProductId the accessory product ID
     * @param fanTypeId the fan type category ID
     * @return true if compatible
     */
    boolean existsByAccessoryProductIdAndCompatibleFanTypeId(Long accessoryProductId, Integer fanTypeId);
    
    /**
     * Check if an accessory is compatible with a specific brand.
     * @param accessoryProductId the accessory product ID
     * @param brandId the brand ID
     * @return true if compatible
     */
    boolean existsByAccessoryProductIdAndCompatibleBrandId(Long accessoryProductId, Integer brandId);
    
    /**
     * Delete all compatibility entries for an accessory product.
     * @param accessoryProductId the accessory product ID
     */
    void deleteByAccessoryProductId(Long accessoryProductId);
    
    /**
     * Count the number of compatibility entries for an accessory product.
     * @param accessoryProductId the accessory product ID
     * @return the number of compatibility entries
     */
    long countByAccessoryProductId(Long accessoryProductId);
    
    /**
     * Count the number of accessories compatible with a specific fan type.
     * @param fanTypeId the fan type category ID
     * @return the number of compatible accessories
     */
    @Query("SELECT COUNT(DISTINCT ac.accessoryProductId) FROM AccessoryCompatibility ac WHERE ac.compatibleFanTypeId = :fanTypeId")
    long countAccessoriesByCompatibleFanTypeId(@Param("fanTypeId") Integer fanTypeId);
}
