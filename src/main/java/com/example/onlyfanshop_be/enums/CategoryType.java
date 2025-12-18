package com.example.onlyfanshop_be.enums;

/**
 * Enum representing the different types of categories in the OnlyFanshop system.
 * Categories are organized by type to support multi-dimensional product classification.
 */
public enum CategoryType {
    /**
     * Fan type categories: standing fan, wall fan, table fan, ceiling fan, etc.
     */
    FAN_TYPE,
    
    /**
     * Space/room categories: living room, bedroom, office, factory, outdoor, etc.
     */
    SPACE,
    
    /**
     * Purpose categories: cooling, energy saving, quiet operation, etc.
     */
    PURPOSE,
    
    /**
     * Technology categories: DC Inverter, Remote Control, WiFi/App Control, etc.
     */
    TECHNOLOGY,
    
    /**
     * Price range categories: under 1M, 1-3M, 3-5M, 5-10M, over 10M VND
     */
    PRICE_RANGE,
    
    /**
     * Customer type categories: household, business, industrial, etc.
     */
    CUSTOMER_TYPE,
    
    /**
     * Product status categories: new, bestseller, on-sale, premium, etc.
     */
    STATUS,
    
    /**
     * Accessory type categories: remote control, power supply, motor, blades, etc.
     */
    ACCESSORY_TYPE,
    
    /**
     * Accessory function categories: cleaning, replacement, upgrade, etc.
     */
    ACCESSORY_FUNCTION;
    
    /**
     * Checks if this category type is valid (non-null).
     * @return true if the category type is valid
     */
    public boolean isValid() {
        return this != null;
    }
    
    /**
     * Checks if this category type is a product type (FAN_TYPE or ACCESSORY_TYPE).
     * Products must have at least one category of these types.
     * @return true if this is a required product type category
     */
    public boolean isProductType() {
        return this == FAN_TYPE || this == ACCESSORY_TYPE;
    }
}
