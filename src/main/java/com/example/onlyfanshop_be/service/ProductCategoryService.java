package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.Category;
import com.example.onlyfanshop_be.entity.ProductCategory;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CategoryRepository;
import com.example.onlyfanshop_be.repository.ProductCategoryRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing product-category relationships.
 * Handles the many-to-many relationship between products and categories,
 * allowing products to be assigned to multiple categories from different types.
 */
@Service
public class ProductCategoryService {

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Assign multiple categories to a product.
     * This method adds new category assignments without removing existing ones.
     * 
     * @param productId the product ID
     * @param categoryIds list of category IDs to assign
     * @throws AppException if product or any category doesn't exist
     */
    @Transactional
    public void assignCategoriesToProduct(Long productId, List<Integer> categoryIds) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        for (Integer categoryId : categoryIds) {
            // Validate category exists
            if (!categoryRepository.existsById(categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }

            // Check if relationship already exists
            if (!productCategoryRepository.existsByProductIdAndCategoryId(productId, categoryId)) {
                ProductCategory productCategory = ProductCategory.builder()
                        .productId(productId)
                        .categoryId(categoryId)
                        .isPrimary(false)
                        .build();
                productCategoryRepository.save(productCategory);
            }
        }
    }


    /**
     * Assign categories to a product, replacing all existing assignments.
     * 
     * @param productId the product ID
     * @param categoryIds list of category IDs to assign
     * @throws AppException if product or any category doesn't exist
     */
    @Transactional
    public void replaceProductCategories(Long productId, List<Integer> categoryIds) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Remove all existing category assignments
        productCategoryRepository.deleteByProductId(productId);

        // Assign new categories
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Integer categoryId : categoryIds) {
                // Validate category exists
                if (!categoryRepository.existsById(categoryId)) {
                    throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
                }

                ProductCategory productCategory = ProductCategory.builder()
                        .productId(productId)
                        .categoryId(categoryId)
                        .isPrimary(false)
                        .build();
                productCategoryRepository.save(productCategory);
            }
        }
    }

    /**
     * Remove a specific category from a product.
     * 
     * @param productId the product ID
     * @param categoryId the category ID to remove
     */
    @Transactional
    public void removeCategoryFromProduct(Long productId, Integer categoryId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productCategoryRepository.deleteByProductIdAndCategoryId(productId, categoryId);
    }

    /**
     * Get all categories assigned to a product.
     * 
     * @param productId the product ID
     * @return list of categories assigned to the product
     */
    public List<Category> getProductCategories(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        List<ProductCategory> productCategories = productCategoryRepository.findByProductIdWithCategory(productId);
        return productCategories.stream()
                .map(ProductCategory::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * Get categories assigned to a product filtered by category type.
     * 
     * @param productId the product ID
     * @param categoryType the category type to filter by
     * @return list of categories of the specified type assigned to the product
     */
    public List<Category> getProductCategoriesByType(Long productId, CategoryType categoryType) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (categoryType == null) {
            throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
        }

        List<ProductCategory> productCategories = productCategoryRepository
                .findByProductIdAndCategoryTypeWithCategory(productId, categoryType);
        return productCategories.stream()
                .map(ProductCategory::getCategory)
                .collect(Collectors.toList());
    }

    /**
     * Check if a product has at least one required category type (FAN_TYPE or ACCESSORY_TYPE).
     * 
     * @param productId the product ID
     * @return true if the product has at least one FAN_TYPE or ACCESSORY_TYPE category
     */
    public boolean hasRequiredCategoryType(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        boolean hasFanType = productCategoryRepository.existsByProductIdAndCategoryType(productId, CategoryType.FAN_TYPE);
        boolean hasAccessoryType = productCategoryRepository.existsByProductIdAndCategoryType(productId, CategoryType.ACCESSORY_TYPE);
        
        return hasFanType || hasAccessoryType;
    }

    /**
     * Validate that a product has at least one required category type.
     * Throws an exception if the validation fails.
     * 
     * @param productId the product ID
     * @throws AppException if the product doesn't have a required category type
     */
    public void validateRequiredCategoryType(Long productId) {
        if (!hasRequiredCategoryType(productId)) {
            throw new AppException(ErrorCode.REQUIRED_CATEGORY_MISSING);
        }
    }

    /**
     * Set the primary category for a product.
     * 
     * @param productId the product ID
     * @param categoryId the category ID to set as primary
     */
    @Transactional
    public void setPrimaryCategory(Long productId, Integer categoryId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Validate category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Reset all existing primary flags for this product
        List<ProductCategory> existingCategories = productCategoryRepository.findByProductId(productId);
        for (ProductCategory pc : existingCategories) {
            if (pc.getIsPrimary()) {
                pc.setIsPrimary(false);
                productCategoryRepository.save(pc);
            }
        }

        // Set the new primary category
        ProductCategory productCategory = productCategoryRepository
                .findByProductIdAndCategoryId(productId, categoryId)
                .orElse(null);

        if (productCategory != null) {
            productCategory.setIsPrimary(true);
            productCategoryRepository.save(productCategory);
        } else {
            // Create new relationship if it doesn't exist
            ProductCategory newProductCategory = ProductCategory.builder()
                    .productId(productId)
                    .categoryId(categoryId)
                    .isPrimary(true)
                    .build();
            productCategoryRepository.save(newProductCategory);
        }
    }

    /**
     * Get the primary category for a product.
     * 
     * @param productId the product ID
     * @return the primary category, or null if none is set
     */
    public Category getPrimaryCategory(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productCategoryRepository.findByProductIdAndIsPrimaryTrue(productId)
                .map(ProductCategory::getCategory)
                .orElse(null);
    }

    /**
     * Get the count of categories assigned to a product.
     * 
     * @param productId the product ID
     * @return the number of categories assigned
     */
    public long getCategoryCount(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productCategoryRepository.countByProductId(productId);
    }

    /**
     * Check if a product has a specific category assigned.
     * 
     * @param productId the product ID
     * @param categoryId the category ID
     * @return true if the product has the category assigned
     */
    public boolean hasCategory(Long productId, Integer categoryId) {
        return productCategoryRepository.existsByProductIdAndCategoryId(productId, categoryId);
    }

    /**
     * Get all product IDs that have a specific category assigned.
     * 
     * @param categoryId the category ID
     * @return list of product IDs
     */
    public List<Long> getProductIdsByCategory(Integer categoryId) {
        List<ProductCategory> productCategories = productCategoryRepository.findByCategoryId(categoryId);
        return productCategories.stream()
                .map(ProductCategory::getProductId)
                .collect(Collectors.toList());
    }

    /**
     * Assign categories to a product with required category type validation.
     * This method replaces all existing assignments and validates that at least
     * one FAN_TYPE or ACCESSORY_TYPE category is included.
     * 
     * @param productId the product ID
     * @param categoryIds list of category IDs to assign
     * @throws AppException if product or any category doesn't exist, or if no required category type is included
     */
    @Transactional
    public void assignCategoriesWithValidation(Long productId, List<Integer> categoryIds) {
        // Validate product exists
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        // Validate that at least one required category type is included
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new AppException(ErrorCode.REQUIRED_CATEGORY_MISSING);
        }

        boolean hasRequiredType = false;
        for (Integer categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            
            if (category.getCategoryType() == CategoryType.FAN_TYPE || 
                category.getCategoryType() == CategoryType.ACCESSORY_TYPE) {
                hasRequiredType = true;
            }
        }

        if (!hasRequiredType) {
            throw new AppException(ErrorCode.REQUIRED_CATEGORY_MISSING);
        }

        // Replace all existing category assignments
        replaceProductCategories(productId, categoryIds);
    }

    /**
     * Check if a list of category IDs contains at least one required category type
     * (FAN_TYPE or ACCESSORY_TYPE) without requiring a product to exist.
     * Useful for validating categories before product creation.
     * 
     * @param categoryIds list of category IDs to check
     * @return true if at least one FAN_TYPE or ACCESSORY_TYPE category is included
     */
    public boolean containsRequiredCategoryType(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return false;
        }

        for (Integer categoryId : categoryIds) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null && 
                (category.getCategoryType() == CategoryType.FAN_TYPE || 
                 category.getCategoryType() == CategoryType.ACCESSORY_TYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that a list of category IDs contains at least one required category type.
     * Throws an exception if validation fails.
     * 
     * @param categoryIds list of category IDs to validate
     * @throws AppException if no required category type is included
     */
    public void validateCategoryIdsContainRequiredType(List<Integer> categoryIds) {
        if (!containsRequiredCategoryType(categoryIds)) {
            throw new AppException(ErrorCode.REQUIRED_CATEGORY_MISSING);
        }
    }
}
