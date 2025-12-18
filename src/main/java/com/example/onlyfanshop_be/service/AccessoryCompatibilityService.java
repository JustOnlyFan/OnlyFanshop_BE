package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.AccessoryCompatibilityRepository;
import com.example.onlyfanshop_be.repository.BrandRepository;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing AccessoryCompatibility entities.
 * Handles CRUD operations for compatibility entries between accessory products
 * and fan types/brands/models.
 * 
 * Requirements: 8.2, 8.3, 8.4, 9.1, 9.2, 9.3
 */
@Service
public class AccessoryCompatibilityService {

    @Autowired
    private AccessoryCompatibilityRepository accessoryCompatibilityRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    // ==================== CRUD Operations ====================

    /**
     * Create a new compatibility entry for an accessory product.
     * 
     * @param compatibility the compatibility entry to create
     * @return the created compatibility entry
     * @throws AppException if the accessory product doesn't exist
     */
    @Transactional
    public AccessoryCompatibility createCompatibility(AccessoryCompatibility compatibility) {
        // Validate accessory product exists
        if (compatibility.getAccessoryProductId() == null) {
            throw new RuntimeException("Accessory product ID is required");
        }
        
        if (!productRepository.existsById(compatibility.getAccessoryProductId().intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Validate fan type category exists if provided
        if (compatibility.getCompatibleFanTypeId() != null) {
            if (!categoryRepository.existsById(compatibility.getCompatibleFanTypeId())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        // Validate brand exists if provided
        if (compatibility.getCompatibleBrandId() != null) {
            if (!brandRepository.existsById(compatibility.getCompatibleBrandId())) {
                throw new RuntimeException("Brand not found with ID: " + compatibility.getCompatibleBrandId());
            }
        }

        return accessoryCompatibilityRepository.save(compatibility);
    }


    /**
     * Get a compatibility entry by its ID.
     * 
     * @param id the compatibility entry ID
     * @return the compatibility entry
     * @throws RuntimeException if not found
     */
    public AccessoryCompatibility getCompatibilityById(Long id) {
        return accessoryCompatibilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compatibility entry not found with ID: " + id));
    }

    /**
     * Get a compatibility entry by its ID, returning Optional.
     * 
     * @param id the compatibility entry ID
     * @return Optional containing the compatibility entry if found
     */
    public Optional<AccessoryCompatibility> findCompatibilityById(Long id) {
        return accessoryCompatibilityRepository.findById(id);
    }

    /**
     * Update an existing compatibility entry.
     * 
     * @param id the compatibility entry ID
     * @param updatedCompatibility the updated compatibility data
     * @return the updated compatibility entry
     * @throws RuntimeException if not found
     */
    @Transactional
    public AccessoryCompatibility updateCompatibility(Long id, AccessoryCompatibility updatedCompatibility) {
        AccessoryCompatibility existing = accessoryCompatibilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compatibility entry not found with ID: " + id));

        // Update fan type if provided
        if (updatedCompatibility.getCompatibleFanTypeId() != null) {
            if (!categoryRepository.existsById(updatedCompatibility.getCompatibleFanTypeId())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            existing.setCompatibleFanTypeId(updatedCompatibility.getCompatibleFanTypeId());
        }

        // Update brand if provided
        if (updatedCompatibility.getCompatibleBrandId() != null) {
            if (!brandRepository.existsById(updatedCompatibility.getCompatibleBrandId())) {
                throw new RuntimeException("Brand not found with ID: " + updatedCompatibility.getCompatibleBrandId());
            }
            existing.setCompatibleBrandId(updatedCompatibility.getCompatibleBrandId());
        }

        // Update model if provided
        if (updatedCompatibility.getCompatibleModel() != null) {
            existing.setCompatibleModel(updatedCompatibility.getCompatibleModel());
        }

        // Update notes if provided
        if (updatedCompatibility.getNotes() != null) {
            existing.setNotes(updatedCompatibility.getNotes());
        }

        return accessoryCompatibilityRepository.save(existing);
    }

    /**
     * Delete a compatibility entry by its ID.
     * 
     * @param id the compatibility entry ID
     * @throws RuntimeException if not found
     */
    @Transactional
    public void deleteCompatibility(Long id) {
        if (!accessoryCompatibilityRepository.existsById(id)) {
            throw new RuntimeException("Compatibility entry not found with ID: " + id);
        }
        accessoryCompatibilityRepository.deleteById(id);
    }

    /**
     * Delete all compatibility entries for an accessory product.
     * 
     * @param accessoryProductId the accessory product ID
     */
    @Transactional
    public void deleteAllByAccessoryProductId(Long accessoryProductId) {
        accessoryCompatibilityRepository.deleteByAccessoryProductId(accessoryProductId);
    }

    // ==================== Query Methods ====================

    /**
     * Get all compatibility entries for an accessory product.
     * 
     * @param accessoryProductId the accessory product ID
     * @return list of compatibility entries
     */
    public List<AccessoryCompatibility> getCompatibilityByProduct(Long accessoryProductId) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return accessoryCompatibilityRepository.findByAccessoryProductId(accessoryProductId);
    }

    /**
     * Get all compatibility entries for an accessory product with related entities loaded.
     * 
     * @param accessoryProductId the accessory product ID
     * @return list of compatibility entries with fan type and brand details
     */
    public List<AccessoryCompatibility> getCompatibilityByProductWithDetails(Long accessoryProductId) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return accessoryCompatibilityRepository.findByAccessoryProductIdWithDetails(accessoryProductId);
    }


    /**
     * Get all accessory product IDs compatible with a specific fan type.
     * 
     * @param fanTypeId the fan type category ID
     * @return list of accessory product IDs
     */
    public List<Long> getAccessoryProductIdsByFanType(Integer fanTypeId) {
        // Validate fan type category exists
        if (!categoryRepository.existsById(fanTypeId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleFanTypeId(fanTypeId);
    }

    /**
     * Get all accessory products compatible with a specific fan type.
     * 
     * @param fanTypeId the fan type category ID
     * @return list of accessory products
     */
    public List<Product> getAccessoriesByFanType(Integer fanTypeId) {
        // Validate fan type category exists
        if (!categoryRepository.existsById(fanTypeId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        List<Long> productIds = accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleFanTypeId(fanTypeId);
        
        return productIds.stream()
                .map(id -> productRepository.findById(id.intValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Get all compatibility entries for a specific fan type.
     * 
     * @param fanTypeId the fan type category ID
     * @return list of compatibility entries
     */
    public List<AccessoryCompatibility> getCompatibilityByFanType(Integer fanTypeId) {
        return accessoryCompatibilityRepository.findByCompatibleFanTypeId(fanTypeId);
    }

    /**
     * Get all accessory product IDs compatible with a specific brand.
     * 
     * @param brandId the brand ID
     * @return list of accessory product IDs
     */
    public List<Long> getAccessoryProductIdsByBrand(Integer brandId) {
        return accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleBrandId(brandId);
    }

    /**
     * Get all accessory products compatible with a specific brand.
     * 
     * @param brandId the brand ID
     * @return list of accessory products
     */
    public List<Product> getAccessoriesByBrand(Integer brandId) {
        List<Long> productIds = accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleBrandId(brandId);
        
        return productIds.stream()
                .map(id -> productRepository.findById(id.intValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Get all accessory product IDs compatible with a specific fan type and brand.
     * 
     * @param fanTypeId the fan type category ID
     * @param brandId the brand ID
     * @return list of accessory product IDs
     */
    public List<Long> getAccessoryProductIdsByFanTypeAndBrand(Integer fanTypeId, Integer brandId) {
        return accessoryCompatibilityRepository.findAccessoryProductIdsByFanTypeAndBrand(fanTypeId, brandId);
    }

    /**
     * Get all accessory products compatible with a specific fan type and brand.
     * 
     * @param fanTypeId the fan type category ID
     * @param brandId the brand ID
     * @return list of accessory products
     */
    public List<Product> getAccessoriesByFanTypeAndBrand(Integer fanTypeId, Integer brandId) {
        List<Long> productIds = accessoryCompatibilityRepository.findAccessoryProductIdsByFanTypeAndBrand(fanTypeId, brandId);
        
        return productIds.stream()
                .map(id -> productRepository.findById(id.intValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Search compatibility entries by model pattern.
     * 
     * @param modelPattern the model pattern to search for
     * @return list of compatibility entries matching the pattern
     */
    public List<AccessoryCompatibility> searchByModel(String modelPattern) {
        if (modelPattern == null || modelPattern.trim().isEmpty()) {
            return List.of();
        }
        return accessoryCompatibilityRepository.findByCompatibleModelContaining(modelPattern.trim());
    }

    // ==================== Validation Methods ====================

    /**
     * Check if an accessory product has any compatibility entries.
     * 
     * @param accessoryProductId the accessory product ID
     * @return true if the accessory has compatibility entries
     */
    public boolean hasCompatibilityEntries(Long accessoryProductId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductId(accessoryProductId);
    }

    /**
     * Check if an accessory is compatible with a specific fan type.
     * 
     * @param accessoryProductId the accessory product ID
     * @param fanTypeId the fan type category ID
     * @return true if compatible
     */
    public boolean isCompatibleWithFanType(Long accessoryProductId, Integer fanTypeId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductIdAndCompatibleFanTypeId(accessoryProductId, fanTypeId);
    }

    /**
     * Check if an accessory is compatible with a specific brand.
     * 
     * @param accessoryProductId the accessory product ID
     * @param brandId the brand ID
     * @return true if compatible
     */
    public boolean isCompatibleWithBrand(Long accessoryProductId, Integer brandId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductIdAndCompatibleBrandId(accessoryProductId, brandId);
    }

    /**
     * Get the count of compatibility entries for an accessory product.
     * 
     * @param accessoryProductId the accessory product ID
     * @return the number of compatibility entries
     */
    public long getCompatibilityCount(Long accessoryProductId) {
        return accessoryCompatibilityRepository.countByAccessoryProductId(accessoryProductId);
    }

    /**
     * Get the count of accessories compatible with a specific fan type.
     * 
     * @param fanTypeId the fan type category ID
     * @return the number of compatible accessories
     */
    public long countAccessoriesByFanType(Integer fanTypeId) {
        return accessoryCompatibilityRepository.countAccessoriesByCompatibleFanTypeId(fanTypeId);
    }

    // ==================== Bulk Operations ====================

    /**
     * Add multiple compatibility entries for an accessory product.
     * 
     * @param accessoryProductId the accessory product ID
     * @param compatibilities list of compatibility entries to add
     * @return list of created compatibility entries
     */
    @Transactional
    public List<AccessoryCompatibility> addCompatibilities(Long accessoryProductId, List<AccessoryCompatibility> compatibilities) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return compatibilities.stream()
                .map(c -> {
                    c.setAccessoryProductId(accessoryProductId);
                    return createCompatibility(c);
                })
                .toList();
    }

    /**
     * Replace all compatibility entries for an accessory product.
     * 
     * @param accessoryProductId the accessory product ID
     * @param compatibilities list of new compatibility entries
     * @return list of created compatibility entries
     */
    @Transactional
    public List<AccessoryCompatibility> replaceCompatibilities(Long accessoryProductId, List<AccessoryCompatibility> compatibilities) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Delete existing entries
        accessoryCompatibilityRepository.deleteByAccessoryProductId(accessoryProductId);

        // Add new entries
        if (compatibilities == null || compatibilities.isEmpty()) {
            return List.of();
        }

        return compatibilities.stream()
                .map(c -> {
                    c.setAccessoryProductId(accessoryProductId);
                    return createCompatibility(c);
                })
                .toList();
    }
}
