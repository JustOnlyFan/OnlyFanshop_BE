package com.example.onlyfanshop_be.enums;

public enum Role {
    ADMIN("Admin", "Administrator with full access"),
    CUSTOMER("Customer", "Regular customer user");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
