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

    public Page<Product> filterProducts(ProductFilterRequest request, Pageable pageable) {
        Specification<Product> spec = buildSpecification(request);
        return productRepository.findAll(spec, pageable);
    }

    private Specification<Product> buildSpecification(ProductFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.active));

            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                Set<Long> productIds = getProductIdsByCategoryIds(
                        request.getCategoryIds(), 
                        request.getIncludeSubcategories() != null ? request.getIncludeSubcategories() : true
                );
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            if (request.getCategoryTypes() != null && !request.getCategoryTypes().isEmpty()) {
                Set<Long> productIds = getProductIdsByCategoryTypes(request.getCategoryTypes());
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            if (request.getBrandIds() != null && !request.getBrandIds().isEmpty()) {
                predicates.add(root.get("brandId").in(request.getBrandIds()));
            }

            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("basePrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("basePrice"), request.getMaxPrice()));
            }

            if (request.getTagCodes() != null && !request.getTagCodes().isEmpty()) {
                Set<Long> productIds = getProductIdsByTagCodes(request.getTagCodes());
                if (!productIds.isEmpty()) {
                    predicates.add(root.get("id").in(productIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            if (request.getCompatibleFanTypeId() != null) {
                List<Long> accessoryProductIds = accessoryCompatibilityRepository
                        .findAccessoryProductIdsByCompatibleFanTypeId(request.getCompatibleFanTypeId());
                if (!accessoryProductIds.isEmpty()) {
                    predicates.add(root.get("id").in(accessoryProductIds));
                } else {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }

            if (request.getSearchQuery() != null && !request.getSearchQuery().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearchQuery().trim().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("shortDescription")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Set<Long> getProductIdsByCategoryIds(List<Integer> categoryIds, boolean includeSubcategories) {
        Set<Integer> allCategoryIds = new HashSet<>();
        
        for (Integer categoryId : categoryIds) {
            allCategoryIds.add(categoryId);
            
            if (includeSubcategories) {
                List<Integer> descendantIds = categoryService.getAllDescendantCategoryIds(categoryId);
                allCategoryIds.addAll(descendantIds);
            }
        }

        List<Long> productIds = productCategoryRepository.findProductIdsByCategoryIds(new ArrayList<>(allCategoryIds));
        return new HashSet<>(productIds);
    }

    private Set<Long> getProductIdsByCategoryTypes(List<CategoryType> categoryTypes) {
        Set<Long> productIds = new HashSet<>();
        
        for (CategoryType type : categoryTypes) {
            List<Long> ids = productCategoryRepository.findProductIdsByCategoryType(type);
            productIds.addAll(ids);
        }
        
        return productIds;
    }

    private Set<Long> getProductIdsByTagCodes(List<String> tagCodes) {
        Set<Long> productIds = new HashSet<>();
        
        for (String tagCode : tagCodes) {
            List<Long> ids = productTagRepository.findProductIdsByActiveTagCode(tagCode);
            productIds.addAll(ids);
        }
        
        return productIds;
    }

    public List<Product> getProductsByCategory(Integer categoryId, boolean includeSubcategories) {
        Set<Long> productIds = getProductIdsByCategoryIds(
                Collections.singletonList(categoryId), 
                includeSubcategories
        );
        
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> intProductIds = productIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
        
        return productRepository.findAllById(intProductIds);
    }

    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        ProductFilterRequest request = ProductFilterRequest.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
        
        Specification<Product> spec = buildSpecification(request);
        return productRepository.findAll(spec);
    }

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

    public List<Integer> getAllCategoryIdsIncludingDescendants(Integer categoryId) {
        return categoryService.getAllDescendantCategoryIds(categoryId);
    }
}
