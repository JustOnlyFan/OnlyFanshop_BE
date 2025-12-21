package com.example.onlyfanshop_be.enums;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for CategoryType enum.
 * 
 * **Feature: expanded-category-system, Property 1: Category Type Validation**
 * **Validates: Requirements 1.1**
 */
class CategoryTypePropertyTest {

    /**
     * **Feature: expanded-category-system, Property 1: Category Type Validation**
     * **Validates: Requirements 1.1**
     * 
     * Property: For any valid CategoryType enum value, the isValid() method SHALL return true.
     * This ensures that all defined category types are considered valid.
     */
    @Property(tries = 100)
    void allCategoryTypesAreValid(@ForAll("validCategoryTypes") CategoryType categoryType) {
        assertThat(categoryType).isNotNull();
        assertThat(categoryType.isValid()).isTrue();
    }

    /**
     * **Feature: expanded-category-system, Property 1: Category Type Validation**
     * **Validates: Requirements 1.1**
     * 
     * Property: The system SHALL have exactly 9 category types as specified in requirements.
     */
    @Property(tries = 100)
    void systemHasExactlyNineCategoryTypes() {
        CategoryType[] allTypes = CategoryType.values();
        assertThat(allTypes).hasSize(9);
    }

    /**
     * **Feature: expanded-category-system, Property 1: Category Type Validation**
     * **Validates: Requirements 1.1**
     * 
     * Property: For any CategoryType, it SHALL be one of the 9 defined types.
     */
    @Property(tries = 100)
    void categoryTypeIsOneOfDefinedTypes(@ForAll("validCategoryTypes") CategoryType categoryType) {
        assertThat(categoryType).isIn(
            CategoryType.FAN_TYPE,
            CategoryType.SPACE,
            CategoryType.PURPOSE,
            CategoryType.TECHNOLOGY,
            CategoryType.PRICE_RANGE,
            CategoryType.CUSTOMER_TYPE,
            CategoryType.STATUS,
            CategoryType.ACCESSORY_TYPE,
            CategoryType.ACCESSORY_FUNCTION
        );
    }

    /**
     * **Feature: expanded-category-system, Property 1: Category Type Validation**
     * **Validates: Requirements 1.1**
     * 
     * Property: Only FAN_TYPE and ACCESSORY_TYPE SHALL be considered product types.
     */
    @Property(tries = 100)
    void onlyFanTypeAndAccessoryTypeAreProductTypes(@ForAll("validCategoryTypes") CategoryType categoryType) {
        boolean isProductType = categoryType.isProductType();
        boolean expectedProductType = (categoryType == CategoryType.FAN_TYPE || categoryType == CategoryType.ACCESSORY_TYPE);
        assertThat(isProductType).isEqualTo(expectedProductType);
    }

    /**
     * **Feature: expanded-category-system, Property 1: Category Type Validation**
     * **Validates: Requirements 1.1**
     * 
     * Property: For any valid string representation of a CategoryType, 
     * valueOf() SHALL return the corresponding enum value.
     */
    @Property(tries = 100)
    void categoryTypeValueOfRoundTrip(@ForAll("validCategoryTypes") CategoryType categoryType) {
        String name = categoryType.name();
        CategoryType parsed = CategoryType.valueOf(name);
        assertThat(parsed).isEqualTo(categoryType);
    }

    @Provide
    Arbitrary<CategoryType> validCategoryTypes() {
        return Arbitraries.of(CategoryType.values());
    }
}
