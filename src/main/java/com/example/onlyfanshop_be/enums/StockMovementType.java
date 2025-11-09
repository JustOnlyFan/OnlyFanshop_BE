package com.example.onlyfanshop_be.enums;

public enum StockMovementType {
    IMPORT("import"),
    export,
    adjustment;

    private final String dbValue;

    StockMovementType() {
        this.dbValue = this.name();
    }

    StockMovementType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static StockMovementType fromDbValue(String dbValue) {
        if (dbValue == null) return null;
        for (StockMovementType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown stock movement type: " + dbValue);
    }
}


