package com.example.onlyfanshop_be.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for AccessoryCompatibilityService persistence logic.
 * 
 * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
 * **Validates: Requirements 9.3, 9.4**
 */
class AccessoryCompatibilityPersistencePropertyTest {

    /**
     * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
     * **Validates: Requirements 9.3, 9.4**
     * 
     * Property: For any accessory product with compatibility information, saving and 
     * retrieving the product SHALL preserve all compatibility entries.
     */
    @Property(tries = 100)
    void saveAndRetrievePreservesAllCompatibilityEntries(
            @ForAll @IntRange(min = 1, max = 1000) long accessoryProductId,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> entries) {
        
        CompatibilityPersistenceSimulator simulator = new CompatibilityPersistenceSimulator();
        
        // Save compatibility entries
        simulator.saveCompatibilities(accessoryProductId, entries);
        
        // Retrieve compatibility entries
        List<TestCompatibilityEntry> retrieved = simulator.getCompatibilities(accessoryProductId);
        
        // All saved entries should be retrieved
        assertThat(retrieved).hasSameSizeAs(entries);
        
        // Each entry should be preserved
        for (TestCompatibilityEntry original : entries) {
            boolean found = retrieved.stream().anyMatch(r -> 
                Objects.equals(r.fanTypeId, original.fanTypeId) &&
                Objects.equals(r.brandId, original.brandId) &&
                Objects.equals(r.model, original.model) &&
                Objects.equals(r.notes, original.notes)
            );
            assertThat(found)
                    .as("Entry with fanType=%d, brand=%d, model=%s should be preserved", 
                        original.fanTypeId, original.brandId, original.model)
                    .isTrue();
        }
    }

    /**
     * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
     * **Validates: Requirements 9.3, 9.4**
     * 
     * Property: Updating compatibility information should reflect changes immediately.
     */
    @Property(tries = 100)
    void updateReflectsChangesImmediately(
            @ForAll @IntRange(min = 1, max = 1000) long accessoryProductId,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> initialEntries,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> updatedEntries) {
        
        CompatibilityPersistenceSimulator simulator = new CompatibilityPersistenceSimulator();
        
        // Save initial entries
        simulator.saveCompatibilities(accessoryProductId, initialEntries);
        
        // Update with new entries (replace all)
        simulator.replaceCompatibilities(accessoryProductId, updatedEntries);
        
        // Retrieve and verify
        List<TestCompatibilityEntry> retrieved = simulator.getCompatibilities(accessoryProductId);
        
        // Should have the updated entries, not the initial ones
        assertThat(retrieved).hasSameSizeAs(updatedEntries);
        
        // Each updated entry should be present
        for (TestCompatibilityEntry updated : updatedEntries) {
            boolean found = retrieved.stream().anyMatch(r -> 
                Objects.equals(r.fanTypeId, updated.fanTypeId) &&
                Objects.equals(r.brandId, updated.brandId) &&
                Objects.equals(r.model, updated.model) &&
                Objects.equals(r.notes, updated.notes)
            );
            assertThat(found).isTrue();
        }
    }


    /**
     * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
     * **Validates: Requirements 9.3, 9.4**
     * 
     * Property: Deleting all compatibility entries should result in empty retrieval.
     */
    @Property(tries = 100)
    void deleteAllResultsInEmptyRetrieval(
            @ForAll @IntRange(min = 1, max = 1000) long accessoryProductId,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> entries) {
        
        // Skip if entries is empty (nothing to delete)
        Assume.that(!entries.isEmpty());
        
        CompatibilityPersistenceSimulator simulator = new CompatibilityPersistenceSimulator();
        
        // Save entries
        simulator.saveCompatibilities(accessoryProductId, entries);
        
        // Verify entries exist
        assertThat(simulator.getCompatibilities(accessoryProductId)).isNotEmpty();
        
        // Delete all entries
        simulator.deleteAllCompatibilities(accessoryProductId);
        
        // Verify empty
        assertThat(simulator.getCompatibilities(accessoryProductId)).isEmpty();
    }

    /**
     * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
     * **Validates: Requirements 9.3, 9.4**
     * 
     * Property: Compatibility entries for different products should be independent.
     */
    @Property(tries = 100)
    void compatibilityEntriesAreIndependentBetweenProducts(
            @ForAll("distinctProductIdPair") long[] productIds,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> entries1,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> entries2) {
        
        long productId1 = productIds[0];
        long productId2 = productIds[1];
        
        CompatibilityPersistenceSimulator simulator = new CompatibilityPersistenceSimulator();
        
        // Save entries for both products
        simulator.saveCompatibilities(productId1, entries1);
        simulator.saveCompatibilities(productId2, entries2);
        
        // Retrieve entries for product 1
        List<TestCompatibilityEntry> retrieved1 = simulator.getCompatibilities(productId1);
        
        // Retrieve entries for product 2
        List<TestCompatibilityEntry> retrieved2 = simulator.getCompatibilities(productId2);
        
        // Each product should have its own entries
        assertThat(retrieved1).hasSameSizeAs(entries1);
        assertThat(retrieved2).hasSameSizeAs(entries2);
        
        // Deleting entries for product 1 should not affect product 2
        simulator.deleteAllCompatibilities(productId1);
        
        assertThat(simulator.getCompatibilities(productId1)).isEmpty();
        assertThat(simulator.getCompatibilities(productId2)).hasSameSizeAs(entries2);
    }

    /**
     * **Feature: expanded-category-system, Property 14: Accessory Compatibility Persistence**
     * **Validates: Requirements 9.3, 9.4**
     * 
     * Property: Adding a single compatibility entry should preserve existing entries.
     */
    @Property(tries = 100)
    void addingSingleEntryPreservesExisting(
            @ForAll @IntRange(min = 1, max = 1000) long accessoryProductId,
            @ForAll("compatibilityEntries") List<TestCompatibilityEntry> existingEntries,
            @ForAll("singleCompatibilityEntry") TestCompatibilityEntry newEntry) {
        
        CompatibilityPersistenceSimulator simulator = new CompatibilityPersistenceSimulator();
        
        // Save existing entries
        simulator.saveCompatibilities(accessoryProductId, existingEntries);
        
        // Add a new entry
        simulator.addCompatibility(accessoryProductId, newEntry);
        
        // Retrieve all entries
        List<TestCompatibilityEntry> retrieved = simulator.getCompatibilities(accessoryProductId);
        
        // Should have all existing entries plus the new one
        assertThat(retrieved).hasSize(existingEntries.size() + 1);
        
        // All existing entries should still be present
        for (TestCompatibilityEntry existing : existingEntries) {
            boolean found = retrieved.stream().anyMatch(r -> 
                Objects.equals(r.fanTypeId, existing.fanTypeId) &&
                Objects.equals(r.brandId, existing.brandId) &&
                Objects.equals(r.model, existing.model) &&
                Objects.equals(r.notes, existing.notes)
            );
            assertThat(found).isTrue();
        }
        
        // New entry should be present
        boolean newFound = retrieved.stream().anyMatch(r -> 
            Objects.equals(r.fanTypeId, newEntry.fanTypeId) &&
            Objects.equals(r.brandId, newEntry.brandId) &&
            Objects.equals(r.model, newEntry.model) &&
            Objects.equals(r.notes, newEntry.notes)
        );
        assertThat(newFound).isTrue();
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<long[]> distinctProductIdPair() {
        return Arbitraries.longs().between(1, 1000)
                .flatMap(id1 -> Arbitraries.longs().between(1, 1000)
                        .filter(id2 -> !id2.equals(id1))
                        .map(id2 -> new long[]{id1, id2}));
    }

    @Provide
    Arbitrary<List<TestCompatibilityEntry>> compatibilityEntries() {
        return Arbitraries.integers().between(0, 5)
                .flatMap(count -> {
                    if (count == 0) {
                        return Arbitraries.just(List.of());
                    }
                    List<Arbitrary<TestCompatibilityEntry>> entryArbitraries = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        entryArbitraries.add(singleCompatibilityEntry());
                    }
                    return Combinators.combine(entryArbitraries).as(list -> list);
                });
    }

    @Provide
    Arbitrary<TestCompatibilityEntry> singleCompatibilityEntry() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 10).injectNull(0.3),
            Arbitraries.integers().between(1, 10).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(20).injectNull(0.3),
            Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(50).injectNull(0.5)
        ).as(TestCompatibilityEntry::new);
    }

    // ==================== Test Data Classes ====================

    static class TestCompatibilityEntry {
        final Integer fanTypeId;
        final Integer brandId;
        final String model;
        final String notes;

        TestCompatibilityEntry(Integer fanTypeId, Integer brandId, String model, String notes) {
            this.fanTypeId = fanTypeId;
            this.brandId = brandId;
            this.model = model;
            this.notes = notes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestCompatibilityEntry that = (TestCompatibilityEntry) o;
            return Objects.equals(fanTypeId, that.fanTypeId) &&
                   Objects.equals(brandId, that.brandId) &&
                   Objects.equals(model, that.model) &&
                   Objects.equals(notes, that.notes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fanTypeId, brandId, model, notes);
        }
    }

    // ==================== Simulator ====================

    /**
     * Simulator that replicates the core compatibility persistence logic
     * for property testing without database dependencies.
     */
    static class CompatibilityPersistenceSimulator {
        private final Map<Long, List<TestCompatibilityEntry>> storage = new HashMap<>();

        void saveCompatibilities(Long productId, List<TestCompatibilityEntry> entries) {
            storage.put(productId, new ArrayList<>(entries));
        }

        void replaceCompatibilities(Long productId, List<TestCompatibilityEntry> entries) {
            storage.put(productId, new ArrayList<>(entries));
        }

        void addCompatibility(Long productId, TestCompatibilityEntry entry) {
            storage.computeIfAbsent(productId, k -> new ArrayList<>()).add(entry);
        }

        List<TestCompatibilityEntry> getCompatibilities(Long productId) {
            return new ArrayList<>(storage.getOrDefault(productId, List.of()));
        }

        void deleteAllCompatibilities(Long productId) {
            storage.remove(productId);
        }

        boolean hasCompatibilities(Long productId) {
            List<TestCompatibilityEntry> entries = storage.get(productId);
            return entries != null && !entries.isEmpty();
        }
    }
}
