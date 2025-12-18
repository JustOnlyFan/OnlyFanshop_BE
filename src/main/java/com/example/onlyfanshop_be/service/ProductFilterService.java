package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.ProductFilterRequest;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.enums.CategoryType;
import com.example.onlyfanshop_be.enums.ProductStatus;
import com.example.onlyfanshop_be.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for filtering products based on multiple criteria.
 * Supports filtering by categories (with hierarchy traversal), brands, price range, tags, and accessory compatibility.
 * 
 * Requirements: 4.1, 5.1, 6.1, 7.1, 8.4
 */
@Service
public class ProductFilterService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductTagRepository productTagRepository;

    @Autowired
    private AccessoryCompatibilityRepository accessoryCompatibilityRepository;

    @Autowired
    private CategoryService categoryService;

    /**
     * Filter products based on multiple criteria with pagination.
     * All filters are combined using AND logic.
     * 
     * @param request the filter request containing all criteria
     * @param pageable pagination information
     * @return page of products matching all criteria
     */
    public Page<Product> filterProducts(ProductFilterRequest request, Pageable pageable) {
        Specification<Product> spec = buildSpecification(request);
        return productRepository.findAll(spec, pageable);
    }


    /**
     * Build a JPA Specification from the filter request.
     * Combines all filter criteria using AND logic.
     */
    private Specification<Product> buildSpecification(ProductFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter for active products
            predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.active));

            // Category filter with hierarchy traversal
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                Set<Long> productIds = getProductIdsByCategoryIds(
                        request.getCategoryIds(), 
                        request.getIncludeSubcategories() != null ? request.getIncludeSubcategories() : true
                );
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    // No products match the category filter
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            // Category type filter
            if (request.getCategoryTypes() != null && !request.getCategoryTypes().isEmpty()) {
                Set<Long> productIds = getProductIdsByCategoryTypes(request.getCategoryTypes());
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            // Brand filter
            if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
                predicates.add(root.get("brandId").in(request.getBrandIds()));
            }

            // Price range filter
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("basePrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("basePrice"), request.getMaxPrice()));
            }

            // Tag filter
            if (request.getTagCodes() != null && !request.getTagCodes().isEmpty()) {
                Set<Long> productIds = getProductIdsByTagCodes(request.getTagCodes());
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            // Accessory compatibility filter
            if (request.getCompatibleFanTypeId() != null) {
                List<Long> accessoryProductIds = accessoryCompatibilityRepository
                        .findAccessoryProductIdsByCompatibleFanTypeId(request.getCompatibleFanTypeId());
                if (!accessoryProductIds.isEmpty()) {
                    predicates.add(root.get("id").in(accessoryProductIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            // Search query filter
            if (request.getSearchQuery() != null && !request.getSearchQuery().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearchQuery().trim().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("shortDescription")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    /**
     * Get product IDs by category IDs, optionally including subcategories.
     * Implements category hierarchy traversal for subcategory inclusion.
     * 
     * Requirements: 4.1 - Products belonging to category and its sub-categories
     * 
     * @param categoryIds list of category IDs
     * @param includeSubcategories whether to include products from subcategories
     * @return set of product IDs matching the category filter
     */
    private Set<Long> getProductIdsByCategoryIds(List<Integer> categoryIds, boolean includeSubcategories) {
        Set<Integer> allCategoryIds = new HashSet<>();
        
        for (Integer categoryId : categoryIds) {
            allCategoryIds.add(categoryId);
            
            if (includeSubcategories) {
                // Get all descendant category IDs using CategoryService
                List<Integer> descendantIds = categoryService.getAllDescendantCategoryIds(categoryId);
                allCategoryIds.addAll(descendantIds);
            }
        }
        
        // Get product IDs from product_categories table
        List<Long> productIds = productCategoryRepository.findProductIdsByCategoryIds(new ArrayList<>(allCategoryIds));
        return new HashSet<>(productIds);
    }

    /**
     * Get product IDs by category types.
     * 
     * @param categoryTypes list of category types
     * @return set of product IDs having at least one category of the specified types
     */
    private Set<Long> getProductIdsByCategoryTypes(List<CategoryType> categoryTypes) {
        Set<Long> productIds = new HashSet<>();
        
        for (CategoryType type : categoryTypes) {
            List<Long> ids = productCategoryRepository.findProductIdsByCategoryType(type);
            productIds.addAll(ids);
        }
        
        return productIds;
    }

    /**
     * Get product IDs by tag codes (currently active tags only).
     * 
     * @param tagCodes list of tag codes
     * @return set of product IDs having at least one of the specified tags
     */
    private Set<Long> getProductIdsByTagCodes(List<String> tagCodes) {
        Set<Long> productIds = new HashSet<>();
        
        for (String tagCode : tagCodes) {
            List<Long> ids = productTagRepository.findProductIdsByActiveTagCode(tagCode);
            productIds.addAll(ids);
        }
        
        return productIds;
    }

    /**
     * Get products by category ID, optionally including subcategories.
     * 
     * Requirements: 4.1 - Category query completeness
     * 
     * @param categoryId the category ID
     * @param includeSubcategories whether to include products from subcategories
     * @return list of products in the category (and subcategories if specified)
     */
    public List<Product> getProductsByCategory(Integer categoryId, boolean includeSubcategories) {
        Set<Long> productIds = getProductIdsByCategoryIds(
                Collections.singletonList(categoryId), 
                includeSubcategories
        );
        
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Convert Long IDs to Integer for ProductRepository
        List<Integer> intProductIds = productIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(intProductIds);
    }

    /**
     * Get products by price range.
     * 
     * Requirements: 7.1 - Price range filter correctness
     * 
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @return list of products within the price range
     */
    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        ProductFilterRequest request = ProductFilterRequest.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
        
        Specification<Product> spec = buildSpecification(request);
        return productRepository.findAll(spec);
    }

    /**
     * Get accessories compatible with a specific fan type.
     * 
     * Requirements: 8.4 - Accessory compatibility filter
     * 
     * @param fanTypeCategoryId the fan type category ID
     * @return list of accessory products compatible with the fan type
     */
    public List<Product> getAccessoriesByCompatibleFanType(Integer fanTypeCategoryId) {
        List<Long> accessoryProductIds = accessoryCompatibilityRepository
                .findAccessoryProductIdsByCompatibleFanTypeId(fanTypeCategoryId);
        
        if (accessoryProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Convert Long IDs to Integer for ProductRepository
        List<Integer> intProductIds = accessoryProductIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(intProductIds);
    }

    /**
     * Get all descendant category IDs for a given category (including itself).
     * This is a convenience method that delegates to CategoryService.
     * 
     * @param categoryId the root category ID
     * @return list of all descendant category IDs
     */
    public List<Integer> getAllCategoryIdsIncludingDescendants(Integer categoryId) {
        return categoryService.getAllDescendantCategoryIds(categoryId);
    }
}
