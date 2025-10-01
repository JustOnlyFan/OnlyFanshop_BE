package com.example.onlyfanshop_be.enums;

public enum OrderStatus {
    PENDING("Pending", "Order is pending confirmation"),
    CONFIRMED("Confirmed", "Order has been confirmed"),
    PROCESSING("Processing", "Order is being processed"),
    SHIPPED("Shipped", "Order has been shipped"),
    DELIVERED("Delivered", "Order has been delivered"),
    CANCELLED("Cancelled", "Order has been cancelled"),
    REFUNDED("Refunded", "Order has been refunded");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
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