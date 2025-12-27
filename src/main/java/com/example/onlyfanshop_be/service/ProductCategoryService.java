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

@Service
public class ProductCategoryService {

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public void assignCategoriesToProduct(Long productId, List<Integer> categoryIds) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        for (Integer categoryId : categoryIds) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }

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

    @Transactional
    public void replaceProductCategories(Long productId, List<Integer> categoryIds) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productCategoryRepository.deleteByProductId(productId);

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

    @Transactional
    public void removeCategoryFromProduct(Long productId, Integer categoryId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        productCategoryRepository.deleteByProductIdAndCategoryId(productId, categoryId);
    }

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

    public List<Category> getProductCategoriesByType(Long productId, CategoryType categoryType) {
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

    public boolean hasRequiredCategoryType(Long productId) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        boolean hasFanType = productCategoryRepository.existsByProductIdAndCategoryType(productId, CategoryType.FAN_TYPE);
        boolean hasAccessoryType = productCategoryRepository.existsByProductIdAndCategoryType(productId, CategoryType.ACCESSORY_TYPE);
        
        return hasFanType || hasAccessoryType;
    }

    public void validateRequiredCategoryType(Long productId) {
        if (!hasRequiredCategoryType(productId)) {
            throw new AppException(ErrorCode.REQUIRED_CATEGORY_MISSING);
        }
    }

    @Transactional
    public void setPrimaryCategory(Long productId, Integer categoryId) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        List<ProductCategory> existingCategories = productCategoryRepository.findByProductId(productId);
        for (ProductCategory pc : existingCategories) {
            if (pc.getIsPrimary()) {
                pc.setIsPrimary(false);
                productCategoryRepository.save(pc);
            }
        }

        ProductCategory productCategory = productCategoryRepository
                .findByProductIdAndCategoryId(productId, categoryId)
                .orElse(null);

        if (productCategory != null) {
            productCategory.setIsPrimary(true);
            productCategoryRepository.save(productCategory);
        } else {
            ProductCategory newProductCategory = ProductCategory.builder()
                    .productId(productId)
                    .categoryId(categoryId)
                    .isPrimary(true)
                    .build();
            productCategoryRepository.save(newProductCategory);
        }
    }

    public Category getPrimaryCategory(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productCategoryRepository.findByProductIdAndIsPrimaryTrue(productId)
                .map(ProductCategory::getCategory)
                .orElse(null);
    }

    public long getCategoryCount(Long productId) {
        // Validate product exists (ProductRepository uses Integer ID)
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

        return productCategoryRepository.countByProductId(productId);
    }

    public boolean hasCategory(Long productId, Integer categoryId) {
        return productCategoryRepository.existsByProductIdAndCategoryId(productId, categoryId);
    }

    public List<Long> getProductIdsByCategory(Integer categoryId) {
        List<ProductCategory> productCategories = productCategoryRepository.findByCategoryId(categoryId);
        return productCategories.stream()
                .map(ProductCategory::getProductId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignCategoriesWithValidation(Long productId, List<Integer> categoryIds) {
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }

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

        replaceProductCategories(productId, categoryIds);
    }

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

}
