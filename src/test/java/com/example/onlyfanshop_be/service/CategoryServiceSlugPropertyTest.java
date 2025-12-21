package com.example.onlyfanshop_be.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.StringLength;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for CategoryService slug auto-generation.
 * 
 * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
 * **Validates: Requirements 1.4**
 */
class CategoryServiceSlugPropertyTest {

    private final CategoryServiceSlugHelper slugHelper = new CategoryServiceSlugHelper();

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any category name, the generated slug SHALL be lowercase.
     */
    @Property(tries = 100)
    void slugIsAlwaysLowercase(@ForAll("validCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        assertThat(slug).isEqualTo(slug.toLowerCase());
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any category name, the generated slug SHALL only contain 
     * URL-safe characters (lowercase letters, numbers, and hyphens).
     */
    @Property(tries = 100)
    void slugContainsOnlyUrlSafeCharacters(@ForAll("validCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        // Slug should only contain lowercase letters, numbers, and hyphens
        assertThat(slug).matches("^[a-z0-9-]*$");
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any category name, the generated slug SHALL not have 
     * leading or trailing hyphens.
     */
    @Property(tries = 100)
    void slugHasNoLeadingOrTrailingHyphens(@ForAll("validCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        if (!slug.isEmpty()) {
            assertThat(slug).doesNotStartWith("-");
            assertThat(slug).doesNotEndWith("-");
        }
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any category name, the generated slug SHALL not have 
     * consecutive hyphens.
     */
    @Property(tries = 100)
    void slugHasNoConsecutiveHyphens(@ForAll("validCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        assertThat(slug).doesNotContain("--");
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any non-empty category name, the generated slug SHALL be non-empty.
     */
    @Property(tries = 100)
    void nonEmptyNameProducesNonEmptySlug(@ForAll("nonEmptyCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        assertThat(slug).isNotEmpty();
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any Vietnamese category name, the generated slug SHALL 
     * convert Vietnamese diacritics to ASCII equivalents.
     */
    @Property(tries = 100)
    void vietnameseCharactersAreConvertedToAscii(@ForAll("vietnameseCategoryNames") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        // Slug should not contain any Vietnamese diacritics
        assertThat(slug).doesNotContainPattern("[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]");
    }

    /**
     * **Feature: expanded-category-system, Property 4: Slug Auto-Generation**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any category name with spaces, the generated slug SHALL 
     * replace spaces with hyphens.
     */
    @Property(tries = 100)
    void spacesAreReplacedWithHyphens(@ForAll("categoryNamesWithSpaces") String categoryName) {
        String slug = slugHelper.generateSlug(categoryName);
        
        // Slug should not contain spaces
        assertThat(slug).doesNotContain(" ");
    }

    @Provide
    Arbitrary<String> validCategoryNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', '-')
                .ofMinLength(1)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<String> nonEmptyCategoryNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> vietnameseCategoryNames() {
        return Arbitraries.of(
                "Quạt đứng",
                "Quạt trần",
                "Quạt bàn",
                "Phòng khách",
                "Phòng ngủ",
                "Văn phòng",
                "Công nghệ DC Inverter",
                "Điều khiển từ xa",
                "Tiết kiệm điện",
                "Êm ái",
                "Quạt treo tường",
                "Quạt công nghiệp",
                "Phụ kiện quạt",
                "Cánh quạt thay thế",
                "Động cơ quạt"
        );
    }

    @Provide
    Arbitrary<String> categoryNamesWithSpaces() {
        return Arbitraries.of(
                "Standing Fan",
                "Ceiling Fan",
                "Table Fan",
                "Living Room",
                "Bed Room",
                "Office Space",
                "DC Inverter Technology",
                "Remote Control",
                "Energy Saving",
                "Wall Mounted Fan"
        );
    }

    /**
     * Helper class that replicates the slug generation logic from CategoryService.
     * This allows testing the slug generation algorithm without Spring context.
     */
    static class CategoryServiceSlugHelper {
        
        public String generateSlug(String categoryName) {
            if (categoryName == null || categoryName.trim().isEmpty()) {
                return "category";
            }
            
            // Convert to lowercase, remove diacritics, replace spaces with hyphens
            String baseSlug = categoryName.toLowerCase()
                    .trim()
                    .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                    .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                    .replaceAll("[ìíịỉĩ]", "i")
                    .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                    .replaceAll("[ùúụủũưừứựửữ]", "u")
                    .replaceAll("[ỳýỵỷỹ]", "y")
                    .replaceAll("[đ]", "d")
                    .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                    .replaceAll("\\s+", "-") // Replace spaces with hyphens
                    .replaceAll("-+", "-") // Replace multiple hyphens with single hyphen
                    .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
            
            if (baseSlug.isEmpty()) {
                baseSlug = "category";
            }
            
            return baseSlug;
        }
    }
}
