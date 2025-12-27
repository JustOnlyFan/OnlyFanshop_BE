package com.example.onlyfanshop_be.enums;

public enum CategoryType {

    FAN_TYPE,

    SPACE,

    PURPOSE,

    TECHNOLOGY,

    PRICE_RANGE,

    CUSTOMER_TYPE,

    STATUS,

    ACCESSORY_TYPE,

    ACCESSORY_FUNCTION;

    public boolean isValid() {
        return this != null;
    }

    public boolean isProductType() {
        return this == FAN_TYPE || this == ACCESSORY_TYPE;
    }
}
