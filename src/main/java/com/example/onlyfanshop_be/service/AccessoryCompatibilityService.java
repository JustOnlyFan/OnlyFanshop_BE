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

    public AccessoryCompatibility getCompatibilityById(Long id) {
        return accessoryCompatibilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compatibility entry not found with ID: " + id));
    }

    public Optional<AccessoryCompatibility> findCompatibilityById(Long id) {
        return accessoryCompatibilityRepository.findById(id);
    }

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

    @Transactional
    public void deleteCompatibility(Long id) {
        if (!accessoryCompatibilityRepository.existsById(id)) {
            throw new RuntimeException("Compatibility entry not found with ID: " + id);
        }
        accessoryCompatibilityRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllByAccessoryProductId(Long accessoryProductId) {
        accessoryCompatibilityRepository.deleteByAccessoryProductId(accessoryProductId);
    }

    public List<AccessoryCompatibility> getCompatibilityByProduct(Long accessoryProductId) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return accessoryCompatibilityRepository.findByAccessoryProductId(accessoryProductId);
    }

    public List<AccessoryCompatibility> getCompatibilityByProductWithDetails(Long accessoryProductId) {
        // Validate product exists
        if (!productRepository.existsById(accessoryProductId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return accessoryCompatibilityRepository.findByAccessoryProductIdWithDetails(accessoryProductId);
    }

    public List<Long> getAccessoryProductIdsByFanType(Integer fanTypeId) {
        // Validate fan type category exists
        if (!categoryRepository.existsById(fanTypeId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleFanTypeId(fanTypeId);
    }

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

    public List<AccessoryCompatibility> getCompatibilityByFanType(Integer fanTypeId) {
        return accessoryCompatibilityRepository.findByCompatibleFanTypeId(fanTypeId);
    }

    public List<Long> getAccessoryProductIdsByBrand(Integer brandId) {
        return accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleBrandId(brandId);
    }

    public List<Product> getAccessoriesByBrand(Integer brandId) {
        List<Long> productIds = accessoryCompatibilityRepository.findAccessoryProductIdsByCompatibleBrandId(brandId);
        
        return productIds.stream()
                .map(id -> productRepository.findById(id.intValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<Long> getAccessoryProductIdsByFanTypeAndBrand(Integer fanTypeId, Integer brandId) {
        return accessoryCompatibilityRepository.findAccessoryProductIdsByFanTypeAndBrand(fanTypeId, brandId);
    }

    public List<Product> getAccessoriesByFanTypeAndBrand(Integer fanTypeId, Integer brandId) {
        List<Long> productIds = accessoryCompatibilityRepository.findAccessoryProductIdsByFanTypeAndBrand(fanTypeId, brandId);
        
        return productIds.stream()
                .map(id -> productRepository.findById(id.intValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<AccessoryCompatibility> searchByModel(String modelPattern) {
        if (modelPattern == null || modelPattern.trim().isEmpty()) {
            return List.of();
        }
        return accessoryCompatibilityRepository.findByCompatibleModelContaining(modelPattern.trim());
    }

    public boolean hasCompatibilityEntries(Long accessoryProductId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductId(accessoryProductId);
    }

    public boolean isCompatibleWithFanType(Long accessoryProductId, Integer fanTypeId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductIdAndCompatibleFanTypeId(accessoryProductId, fanTypeId);
    }

    public boolean isCompatibleWithBrand(Long accessoryProductId, Integer brandId) {
        return accessoryCompatibilityRepository.existsByAccessoryProductIdAndCompatibleBrandId(accessoryProductId, brandId);
    }

    public long getCompatibilityCount(Long accessoryProductId) {
        return accessoryCompatibilityRepository.countByAccessoryProductId(accessoryProductId);
    }

    public long countAccessoriesByFanType(Integer fanTypeId) {
        return accessoryCompatibilityRepository.countAccessoriesByCompatibleFanTypeId(fanTypeId);
    }

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
