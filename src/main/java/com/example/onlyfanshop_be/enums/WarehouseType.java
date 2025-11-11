package com.example.onlyfanshop_be.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WarehouseType {
    MAIN("main"),          // Kho hàng tổng
    REGIONAL("regional"),  // Kho khu vực
    BRANCH("branch");      // Kho chi nhánh

    private final String dbValue;

    WarehouseType(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static WarehouseType fromDbValue(String dbValue) {
        if (dbValue == null) return null;
        // Try to match by dbValue first (lowercase)
        for (WarehouseType type : values()) {
            if (type.dbValue.equalsIgnoreCase(dbValue)) {
                return type;
            }
        }
        // Try to match by enum name (uppercase) for backward compatibility
        try {
            return WarehouseType.valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown warehouse type: " + dbValue);
        }
    }
}

