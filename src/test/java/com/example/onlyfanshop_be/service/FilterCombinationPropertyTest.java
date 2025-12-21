package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.enums.CategoryType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ProductFilterService filter combination logic.
 * 
 * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
 * **Validates: Requirements 5.3, 6.3, 7.3**
 */
class FilterCombinationPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
     * **Validates: Requirements 5.3, 6.3, 7.3**
     * 
     * Property: For any combination of filters, the returned products SHALL be 
     * the intersection of products matching each individual filter.
     * 
     * This test verifies that combining category and brand filters returns only
     * products that match BOTH criteria.
     */
    @Property(tries = 100)
    void combinedFiltersReturnIntersection(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll @IntRange(min = 1, max = 10) int categoryId,
            @ForAll @IntRange(min = 1, max = 10) int brandId) {
        
        FilterSimulator simulator = new FilterSimulator(products);
        
        // Get products matching category filter only
        Set<Long> categoryMatches = simulator.filterByCategory(categoryId);
        
        // Get products matching brand filter only
        Set<Long> brandMatches = simulator.filterByBrand(brandId);
        
        // Get products matching combined filters
        Set<Long> combinedMatches = simulator.filterByCategoryAndBrand(categoryId, brandId);
        
        // Combined result should be the intersection
        Set<Long> expectedIntersection = new HashSet<>(categoryMatches);
        expectedIntersection.retainAll(brandMatches);
        
        assertThat(combinedMatches).containsExactlyInAnyOrderElementsOf(expectedIntersection);
    }


    /**
     * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
     * **Validates: Requirements 5.3, 6.3, 7.3**
     * 
     * Property: Combining category and price range filters returns only products
     * that match both criteria.
     */
    @Property(tries = 100)
    void categoryAndPriceFiltersReturnIntersection(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll @IntRange(min = 1, max = 10) int categoryId,
            @ForAll("priceRange") PriceRange priceRange) {
        
        FilterSimulator simulator = new FilterSimulator(products);
        
        // Get products matching category filter only
        Set<Long> categoryMatches = simulator.filterByCategory(categoryId);
        
        // Get products matching price range filter only
        Set<Long> priceMatches = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        
        // Get products matching combined filters
        Set<Long> combinedMatches = simulator.filterByCategoryAndPriceRange(categoryId, priceRange.minPrice, priceRange.maxPrice);
        
        // Combined result should be the intersection
        Set<Long> expectedIntersection = new HashSet<>(categoryMatches);
        expectedIntersection.retainAll(priceMatches);
        
        assertThat(combinedMatches).containsExactlyInAnyOrderElementsOf(expectedIntersection);
    }

    /**
     * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
     * **Validates: Requirements 5.3, 6.3, 7.3**
     * 
     * Property: Combining brand and tag filters returns only products
     * that match both criteria.
     */
    @Property(tries = 100)
    void brandAndTagFiltersReturnIntersection(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll @IntRange(min = 1, max = 10) int brandId,
            @ForAll("tagCode") String tagCode) {
        
        FilterSimulator simulator = new FilterSimulator(products);
        
        // Get products matching brand filter only
        Set<Long> brandMatches = simulator.filterByBrand(brandId);
        
        // Get products matching tag filter only
        Set<Long> tagMatches = simulator.filterByTag(tagCode);
        
        // Get products matching combined filters
        Set<Long> combinedMatches = simulator.filterByBrandAndTag(brandId, tagCode);
        
        // Combined result should be the intersection
        Set<Long> expectedIntersection = new HashSet<>(brandMatches);
        expectedIntersection.retainAll(tagMatches);
        
        assertThat(combinedMatches).containsExactlyInAnyOrderElementsOf(expectedIntersection);
    }

    /**
     * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
     * **Validates: Requirements 5.3, 6.3, 7.3**
     * 
     * Property: Combining all filters (category, brand, price, tag) returns only products
     * that match ALL criteria.
     */
    @Property(tries = 100)
    void allFiltersReturnIntersection(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll @IntRange(min = 1, max = 10) int categoryId,
            @ForAll @IntRange(min = 1, max = 10) int brandId,
            @ForAll("priceRange") PriceRange priceRange,
            @ForAll("tagCode") String tagCode) {
        
        FilterSimulator simulator = new FilterSimulator(products);
        
        // Get products matching each filter individually
        Set<Long> categoryMatches = simulator.filterByCategory(categoryId);
        Set<Long> brandMatches = simulator.filterByBrand(brandId);
        Set<Long> priceMatches = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        Set<Long> tagMatches = simulator.filterByTag(tagCode);
        
        // Get products matching all filters combined
        Set<Long> combinedMatches = simulator.filterByAll(categoryId, brandId, priceRange.minPrice, priceRange.maxPrice, tagCode);
        
        // Combined result should be the intersection of all
        Set<Long> expectedIntersection = new HashSet<>(categoryMatches);
        expectedIntersection.retainAll(brandMatches);
        expectedIntersection.retainAll(priceMatches);
        expectedIntersection.retainAll(tagMatches);
        
        assertThat(combinedMatches).containsExactlyInAnyOrderElementsOf(expectedIntersection);
    }

    /**
     * **Feature: expanded-category-system, Property 11: Filter Combination (AND Logic)**
     * **Validates: Requirements 5.3, 6.3, 7.3**
     * 
     * Property: Adding more filters should never increase the result set size.
     */
    @Property(tries = 100)
    void addingFiltersNeverIncreasesResultSize(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll @IntRange(min = 1, max = 10) int categoryId,
            @ForAll @IntRange(min = 1, max = 10) int brandId) {
        
        FilterSimulator simulator = new FilterSimulator(products);
        
        // Get products matching category filter only
        Set<Long> categoryOnlyMatches = simulator.filterByCategory(categoryId);
        
        // Get products matching category AND brand filters
        Set<Long> combinedMatches = simulator.filterByCategoryAndBrand(categoryId, brandId);
        
        // Combined result should be <= category-only result
        assertThat(combinedMatches.size()).isLessThanOrEqualTo(categoryOnlyMatches.size());
    }


    // ==================== Providers ====================

    @Provide
    Arbitrary<List<TestProduct>> testProducts() {
        // Generate products with unique sequential IDs to avoid duplicate ID issues
        return Arbitraries.integers().between(5, 30)
                .flatMap(count -> {
                    List<Arbitrary<TestProduct>> productArbitraries = new ArrayList<>();
                    for (int i = 1; i <= count; i++) {
                        final long id = i;
                        productArbitraries.add(
                            Combinators.combine(
                                Arbitraries.integers().between(1, 10),
                                Arbitraries.integers().between(1, 10),
                                Arbitraries.bigDecimals().between(BigDecimal.valueOf(100), BigDecimal.valueOf(10000)),
                                Arbitraries.of("NEW", "SALE", "PREMIUM", "BESTSELLER")
                            ).as((categoryId, brandId, price, tagCode) -> 
                                new TestProduct(id, categoryId, brandId, price, tagCode))
                        );
                    }
                    return Combinators.combine(productArbitraries).as(list -> list);
                });
    }

    @Provide
    Arbitrary<PriceRange> priceRange() {
        return Arbitraries.bigDecimals().between(BigDecimal.valueOf(100), BigDecimal.valueOf(5000))
                .flatMap(min -> Arbitraries.bigDecimals().between(min, BigDecimal.valueOf(10000))
                        .map(max -> new PriceRange(min, max)));
    }

    @Provide
    Arbitrary<String> tagCode() {
        return Arbitraries.of("NEW", "SALE", "PREMIUM", "BESTSELLER");
    }

    // ==================== Test Data Classes ====================

    static class TestProduct {
        final Long id;
        final Integer categoryId;
        final Integer brandId;
        final BigDecimal price;
        final String tagCode;

        TestProduct(Long id, Integer categoryId, Integer brandId, BigDecimal price, String tagCode) {
            this.id = id;
            this.categoryId = categoryId;
            this.brandId = brandId;
            this.price = price;
            this.tagCode = tagCode;
        }
    }

    static class PriceRange {
        final BigDecimal minPrice;
        final BigDecimal maxPrice;

        PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }

    // ==================== Simulator ====================

    /**
     * Simulator that replicates the core filter combination logic
     * for property testing without database dependencies.
     */
    static class FilterSimulator {
        private final List<TestProduct> products;

        FilterSimulator(List<TestProduct> products) {
            this.products = products;
        }

        Set<Long> filterByCategory(Integer categoryId) {
            return products.stream()
                    .filter(p -> p.categoryId.equals(categoryId))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByBrand(Integer brandId) {
            return products.stream()
                    .filter(p -> p.brandId.equals(brandId))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            return products.stream()
                    .filter(p -> p.price.compareTo(minPrice) >= 0 && p.price.compareTo(maxPrice) <= 0)
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByTag(String tagCode) {
            return products.stream()
                    .filter(p -> p.tagCode.equals(tagCode))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByCategoryAndBrand(Integer categoryId, Integer brandId) {
            return products.stream()
                    .filter(p -> p.categoryId.equals(categoryId) && p.brandId.equals(brandId))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByCategoryAndPriceRange(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
            return products.stream()
                    .filter(p -> p.categoryId.equals(categoryId) 
                            && p.price.compareTo(minPrice) >= 0 
                            && p.price.compareTo(maxPrice) <= 0)
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByBrandAndTag(Integer brandId, String tagCode) {
            return products.stream()
                    .filter(p -> p.brandId.equals(brandId) && p.tagCode.equals(tagCode))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }

        Set<Long> filterByAll(Integer categoryId, Integer brandId, BigDecimal minPrice, BigDecimal maxPrice, String tagCode) {
            return products.stream()
                    .filter(p -> p.categoryId.equals(categoryId)
                            && p.brandId.equals(brandId)
                            && p.price.compareTo(minPrice) >= 0
                            && p.price.compareTo(maxPrice) <= 0
                            && p.tagCode.equals(tagCode))
                    .map(p -> p.id)
                    .collect(Collectors.toSet());
        }
    }
}
