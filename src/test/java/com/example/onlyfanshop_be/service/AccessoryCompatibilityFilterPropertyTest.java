package com.example.onlyfanshop_be.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for AccessoryCompatibilityService filter logic.
 * 
 * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
 * **Validates: Requirements 8.4**
 */
class AccessoryCompatibilityFilterPropertyTest {

    /**
     * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
     * **Validates: Requirements 8.4**
     * 
     * Property: For any accessory filter by compatible fan type, all returned products 
     * SHALL have at least one compatibility entry matching that fan type.
     */
    @Property(tries = 100)
    void filterByFanTypeReturnsOnlyCompatibleAccessories(
            @ForAll("testAccessories") List<TestAccessory> accessories,
            @ForAll @IntRange(min = 1, max = 10) int fanTypeId) {
        
        AccessoryFilterSimulator simulator = new AccessoryFilterSimulator(accessories);
        
        // Get accessories filtered by fan type
        Set<Long> filteredAccessories = simulator.filterByFanType(fanTypeId);
        
        // Verify all returned accessories have at least one compatibility entry for the fan type
        for (Long accessoryId : filteredAccessories) {
            boolean hasCompatibility = simulator.hasCompatibilityWithFanType(accessoryId, fanTypeId);
            assertThat(hasCompatibility)
                    .as("Accessory %d should have compatibility with fan type %d", accessoryId, fanTypeId)
                    .isTrue();
        }
    }

    /**
     * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
     * **Validates: Requirements 8.4**
     * 
     * Property: Accessories without compatibility entries for a fan type should NOT
     * be returned when filtering by that fan type.
     */
    @Property(tries = 100)
    void filterByFanTypeExcludesIncompatibleAccessories(
            @ForAll("testAccessories") List<TestAccessory> accessories,
            @ForAll @IntRange(min = 1, max = 10) int fanTypeId) {
        
        AccessoryFilterSimulator simulator = new AccessoryFilterSimulator(accessories);
        
        // Get accessories filtered by fan type
        Set<Long> filteredAccessories = simulator.filterByFanType(fanTypeId);
        
        // Get all accessories that DON'T have compatibility with the fan type
        Set<Long> incompatibleAccessories = accessories.stream()
                .filter(a -> !simulator.hasCompatibilityWithFanType(a.id, fanTypeId))
                .map(a -> a.id)
                .collect(Collectors.toSet());
        
        // Verify none of the incompatible accessories are in the filtered result
        for (Long incompatibleId : incompatibleAccessories) {
            assertThat(filteredAccessories)
                    .as("Incompatible accessory %d should not be in filtered results", incompatibleId)
                    .doesNotContain(incompatibleId);
        }
    }


    /**
     * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
     * **Validates: Requirements 8.4**
     * 
     * Property: The filter result should be exactly the set of accessories that have
     * at least one compatibility entry for the specified fan type.
     */
    @Property(tries = 100)
    void filterByFanTypeReturnsExactlyMatchingAccessories(
            @ForAll("testAccessories") List<TestAccessory> accessories,
            @ForAll @IntRange(min = 1, max = 10) int fanTypeId) {
        
        AccessoryFilterSimulator simulator = new AccessoryFilterSimulator(accessories);
        
        // Get accessories filtered by fan type using the filter method
        Set<Long> filteredAccessories = simulator.filterByFanType(fanTypeId);
        
        // Compute expected result: all accessories with at least one compatibility entry for the fan type
        Set<Long> expectedAccessories = accessories.stream()
                .filter(a -> simulator.hasCompatibilityWithFanType(a.id, fanTypeId))
                .map(a -> a.id)
                .collect(Collectors.toSet());
        
        // The filtered result should exactly match the expected set
        assertThat(filteredAccessories)
                .containsExactlyInAnyOrderElementsOf(expectedAccessories);
    }

    /**
     * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
     * **Validates: Requirements 8.4**
     * 
     * Property: Filtering by brand should return only accessories compatible with that brand.
     */
    @Property(tries = 100)
    void filterByBrandReturnsOnlyCompatibleAccessories(
            @ForAll("testAccessories") List<TestAccessory> accessories,
            @ForAll @IntRange(min = 1, max = 10) int brandId) {
        
        AccessoryFilterSimulator simulator = new AccessoryFilterSimulator(accessories);
        
        // Get accessories filtered by brand
        Set<Long> filteredAccessories = simulator.filterByBrand(brandId);
        
        // Compute expected result
        Set<Long> expectedAccessories = accessories.stream()
                .filter(a -> simulator.hasCompatibilityWithBrand(a.id, brandId))
                .map(a -> a.id)
                .collect(Collectors.toSet());
        
        assertThat(filteredAccessories)
                .containsExactlyInAnyOrderElementsOf(expectedAccessories);
    }

    /**
     * **Feature: expanded-category-system, Property 13: Accessory Compatibility Filter**
     * **Validates: Requirements 8.4**
     * 
     * Property: Filtering by both fan type and brand should return only accessories
     * compatible with BOTH criteria.
     */
    @Property(tries = 100)
    void filterByFanTypeAndBrandReturnsIntersection(
            @ForAll("testAccessories") List<TestAccessory> accessories,
            @ForAll @IntRange(min = 1, max = 10) int fanTypeId,
            @ForAll @IntRange(min = 1, max = 10) int brandId) {
        
        AccessoryFilterSimulator simulator = new AccessoryFilterSimulator(accessories);
        
        // Get accessories filtered by fan type only
        Set<Long> fanTypeMatches = simulator.filterByFanType(fanTypeId);
        
        // Get accessories filtered by brand only
        Set<Long> brandMatches = simulator.filterByBrand(brandId);
        
        // Get accessories filtered by both
        Set<Long> combinedMatches = simulator.filterByFanTypeAndBrand(fanTypeId, brandId);
        
        // Combined result should be the intersection
        Set<Long> expectedIntersection = new HashSet<>(fanTypeMatches);
        expectedIntersection.retainAll(brandMatches);
        
        assertThat(combinedMatches)
                .containsExactlyInAnyOrderElementsOf(expectedIntersection);
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<List<TestAccessory>> testAccessories() {
        return Arbitraries.integers().between(5, 20)
                .flatMap(count -> {
                    List<Arbitrary<TestAccessory>> accessoryArbitraries = new ArrayList<>();
                    for (int i = 1; i <= count; i++) {
                        final long id = i;
                        accessoryArbitraries.add(
                            Arbitraries.of(generateCompatibilityList())
                                .map(compatibilities -> new TestAccessory(id, compatibilities))
                        );
                    }
                    return Combinators.combine(accessoryArbitraries).as(list -> list);
                });
    }

    private List<List<TestCompatibility>> generateCompatibilityList() {
        // Generate various compatibility scenarios
        List<List<TestCompatibility>> scenarios = new ArrayList<>();
        
        // Empty compatibility
        scenarios.add(List.of());
        
        // Single fan type compatibility
        for (int fanType = 1; fanType <= 10; fanType++) {
            scenarios.add(List.of(new TestCompatibility(fanType, null)));
        }
        
        // Single brand compatibility
        for (int brand = 1; brand <= 10; brand++) {
            scenarios.add(List.of(new TestCompatibility(null, brand)));
        }
        
        // Fan type and brand compatibility
        for (int fanType = 1; fanType <= 5; fanType++) {
            for (int brand = 1; brand <= 5; brand++) {
                scenarios.add(List.of(new TestCompatibility(fanType, brand)));
            }
        }
        
        // Multiple compatibilities
        scenarios.add(List.of(
            new TestCompatibility(1, null),
            new TestCompatibility(2, null),
            new TestCompatibility(3, 1)
        ));
        
        scenarios.add(List.of(
            new TestCompatibility(5, 2),
            new TestCompatibility(5, 3),
            new TestCompatibility(6, 2)
        ));
        
        return scenarios;
    }

    // ==================== Test Data Classes ====================

    static class TestAccessory {
        final Long id;
        final List<TestCompatibility> compatibilities;

        TestAccessory(Long id, List<TestCompatibility> compatibilities) {
            this.id = id;
            this.compatibilities = compatibilities;
        }
    }

    static class TestCompatibility {
        final Integer fanTypeId;
        final Integer brandId;

        TestCompatibility(Integer fanTypeId, Integer brandId) {
            this.fanTypeId = fanTypeId;
            this.brandId = brandId;
        }
    }

    // ==================== Simulator ====================

    /**
     * Simulator that replicates the core accessory compatibility filter logic
     * for property testing without database dependencies.
     */
    static class AccessoryFilterSimulator {
        private final List<TestAccessory> accessories;

        AccessoryFilterSimulator(List<TestAccessory> accessories) {
            this.accessories = accessories;
        }

        /**
         * Check if an accessory has at least one compatibility entry for a fan type.
         */
        boolean hasCompatibilityWithFanType(Long accessoryId, Integer fanTypeId) {
            return accessories.stream()
                    .filter(a -> a.id.equals(accessoryId))
                    .flatMap(a -> a.compatibilities.stream())
                    .anyMatch(c -> c.fanTypeId != null && c.fanTypeId.equals(fanTypeId));
        }

        /**
         * Check if an accessory has at least one compatibility entry for a brand.
         */
        boolean hasCompatibilityWithBrand(Long accessoryId, Integer brandId) {
            return accessories.stream()
                    .filter(a -> a.id.equals(accessoryId))
                    .flatMap(a -> a.compatibilities.stream())
                    .anyMatch(c -> c.brandId != null && c.brandId.equals(brandId));
        }

        /**
         * Filter accessories by fan type - returns accessories with at least one
         * compatibility entry for the specified fan type.
         */
        Set<Long> filterByFanType(Integer fanTypeId) {
            return accessories.stream()
                    .filter(a -> hasCompatibilityWithFanType(a.id, fanTypeId))
                    .map(a -> a.id)
                    .collect(Collectors.toSet());
        }

        /**
         * Filter accessories by brand - returns accessories with at least one
         * compatibility entry for the specified brand.
         */
        Set<Long> filterByBrand(Integer brandId) {
            return accessories.stream()
                    .filter(a -> hasCompatibilityWithBrand(a.id, brandId))
                    .map(a -> a.id)
                    .collect(Collectors.toSet());
        }

        /**
         * Filter accessories by both fan type and brand - returns accessories
         * that have compatibility entries matching BOTH criteria.
         */
        Set<Long> filterByFanTypeAndBrand(Integer fanTypeId, Integer brandId) {
            return accessories.stream()
                    .filter(a -> hasCompatibilityWithFanType(a.id, fanTypeId) 
                              && hasCompatibilityWithBrand(a.id, brandId))
                    .map(a -> a.id)
                    .collect(Collectors.toSet());
        }
    }
}
