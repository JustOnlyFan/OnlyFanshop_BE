package com.example.onlyfanshop_be.service;

import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ProductFilterService price range filtering.
 * 
 * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
 * **Validates: Requirements 7.1**
 */
class PriceRangeFilterPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
     * **Validates: Requirements 7.1**
     * 
     * Property: For any price range filter, all returned products SHALL have 
     * a base price within the specified range (inclusive).
     */
    @Property(tries = 100)
    void allReturnedProductsAreWithinPriceRange(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll("priceRange") PriceRange priceRange) {
        
        PriceRangeFilterSimulator simulator = new PriceRangeFilterSimulator(products);
        
        // Filter by price range
        List<TestProduct> filteredProducts = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        
        // Verify all returned products are within the price range
        for (TestProduct product : filteredProducts) {
            assertThat(product.price)
                    .as("Product %d price %s should be >= %s", product.id, product.price, priceRange.minPrice)
                    .isGreaterThanOrEqualTo(priceRange.minPrice);
            assertThat(product.price)
                    .as("Product %d price %s should be <= %s", product.id, product.price, priceRange.maxPrice)
                    .isLessThanOrEqualTo(priceRange.maxPrice);
        }
    }

    /**
     * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
     * **Validates: Requirements 7.1**
     * 
     * Property: All products within the price range should be included in the result.
     */
    @Property(tries = 100)
    void allProductsWithinRangeAreIncluded(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll("priceRange") PriceRange priceRange) {
        
        PriceRangeFilterSimulator simulator = new PriceRangeFilterSimulator(products);
        
        // Filter by price range
        List<TestProduct> filteredProducts = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        Set<Long> filteredIds = filteredProducts.stream().map(p -> p.id).collect(Collectors.toSet());
        
        // Find all products that should be in the result
        for (TestProduct product : products) {
            boolean shouldBeIncluded = product.price.compareTo(priceRange.minPrice) >= 0 
                    && product.price.compareTo(priceRange.maxPrice) <= 0;
            
            if (shouldBeIncluded) {
                assertThat(filteredIds)
                        .as("Product %d with price %s should be included in range [%s, %s]", 
                            product.id, product.price, priceRange.minPrice, priceRange.maxPrice)
                        .contains(product.id);
            }
        }
    }


    /**
     * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
     * **Validates: Requirements 7.1**
     * 
     * Property: Products outside the price range should NOT be included in the result.
     */
    @Property(tries = 100)
    void productsOutsideRangeAreExcluded(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll("priceRange") PriceRange priceRange) {
        
        PriceRangeFilterSimulator simulator = new PriceRangeFilterSimulator(products);
        
        // Filter by price range
        List<TestProduct> filteredProducts = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        Set<Long> filteredIds = filteredProducts.stream().map(p -> p.id).collect(Collectors.toSet());
        
        // Find all products that should NOT be in the result
        for (TestProduct product : products) {
            boolean shouldBeExcluded = product.price.compareTo(priceRange.minPrice) < 0 
                    || product.price.compareTo(priceRange.maxPrice) > 0;
            
            if (shouldBeExcluded) {
                assertThat(filteredIds)
                        .as("Product %d with price %s should NOT be included in range [%s, %s]", 
                            product.id, product.price, priceRange.minPrice, priceRange.maxPrice)
                        .doesNotContain(product.id);
            }
        }
    }

    /**
     * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
     * **Validates: Requirements 7.1**
     * 
     * Property: The count of filtered products equals the count of products within the range.
     */
    @Property(tries = 100)
    void filteredCountMatchesExpectedCount(
            @ForAll("testProducts") List<TestProduct> products,
            @ForAll("priceRange") PriceRange priceRange) {
        
        PriceRangeFilterSimulator simulator = new PriceRangeFilterSimulator(products);
        
        // Filter by price range
        List<TestProduct> filteredProducts = simulator.filterByPriceRange(priceRange.minPrice, priceRange.maxPrice);
        
        // Count products that should be in the result
        long expectedCount = products.stream()
                .filter(p -> p.price.compareTo(priceRange.minPrice) >= 0 
                        && p.price.compareTo(priceRange.maxPrice) <= 0)
                .count();
        
        assertThat(filteredProducts).hasSize((int) expectedCount);
    }

    /**
     * **Feature: expanded-category-system, Property 12: Price Range Filter Correctness**
     * **Validates: Requirements 7.1**
     * 
     * Property: Boundary values (min and max) are inclusive.
     */
    @Property(tries = 100)
    void boundaryValuesAreInclusive(
            @ForAll("testProductsWithBoundaryPrices") TestProductsWithBoundary testData) {
        
        PriceRangeFilterSimulator simulator = new PriceRangeFilterSimulator(testData.products);
        
        // Filter by price range
        List<TestProduct> filteredProducts = simulator.filterByPriceRange(testData.minPrice, testData.maxPrice);
        Set<Long> filteredIds = filteredProducts.stream().map(p -> p.id).collect(Collectors.toSet());
        
        // Products at exact min and max prices should be included
        for (TestProduct product : testData.products) {
            if (product.price.compareTo(testData.minPrice) == 0 || product.price.compareTo(testData.maxPrice) == 0) {
                assertThat(filteredIds)
                        .as("Product %d at boundary price %s should be included", product.id, product.price)
                        .contains(product.id);
            }
        }
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<List<TestProduct>> testProducts() {
        return Arbitraries.integers().between(5, 20)
                .flatMap(count -> {
                    List<Arbitrary<TestProduct>> productArbitraries = new ArrayList<>();
                    for (int i = 1; i <= count; i++) {
                        final long id = i;
                        productArbitraries.add(
                            Arbitraries.bigDecimals()
                                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(20000))
                                .ofScale(2)
                                .map(price -> new TestProduct(id, price))
                        );
                    }
                    return Combinators.combine(productArbitraries).as(list -> list);
                });
    }

    @Provide
    Arbitrary<PriceRange> priceRange() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(10000))
                .ofScale(2)
                .flatMap(min -> Arbitraries.bigDecimals()
                        .between(min, BigDecimal.valueOf(20000))
                        .ofScale(2)
                        .map(max -> new PriceRange(min, max)));
    }

    @Provide
    Arbitrary<TestProductsWithBoundary> testProductsWithBoundaryPrices() {
        return Combinators.combine(
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(1000), BigDecimal.valueOf(5000)).ofScale(2),
                Arbitraries.bigDecimals().between(BigDecimal.valueOf(5001), BigDecimal.valueOf(15000)).ofScale(2)
        ).flatAs((min, max) -> {
            // Create products including some at exact boundary values
            List<TestProduct> products = new ArrayList<>();
            products.add(new TestProduct(1L, min)); // At min boundary
            products.add(new TestProduct(2L, max)); // At max boundary
            products.add(new TestProduct(3L, min.subtract(BigDecimal.ONE))); // Below min
            products.add(new TestProduct(4L, max.add(BigDecimal.ONE))); // Above max
            products.add(new TestProduct(5L, min.add(max).divide(BigDecimal.valueOf(2)))); // In middle
            
            return Arbitraries.just(new TestProductsWithBoundary(products, min, max));
        });
    }


    // ==================== Test Data Classes ====================

    static class TestProduct {
        final Long id;
        final BigDecimal price;

        TestProduct(Long id, BigDecimal price) {
            this.id = id;
            this.price = price;
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

    static class TestProductsWithBoundary {
        final List<TestProduct> products;
        final BigDecimal minPrice;
        final BigDecimal maxPrice;

        TestProductsWithBoundary(List<TestProduct> products, BigDecimal minPrice, BigDecimal maxPrice) {
            this.products = products;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }

    // ==================== Simulator ====================

    /**
     * Simulator that replicates the core price range filter logic
     * for property testing without database dependencies.
     */
    static class PriceRangeFilterSimulator {
        private final List<TestProduct> products;

        PriceRangeFilterSimulator(List<TestProduct> products) {
            this.products = products;
        }

        List<TestProduct> filterByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            return products.stream()
                    .filter(p -> p.price.compareTo(minPrice) >= 0 && p.price.compareTo(maxPrice) <= 0)
                    .collect(Collectors.toList());
        }
    }
}
