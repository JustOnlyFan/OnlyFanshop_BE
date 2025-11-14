package com.example.onlyfanshop_be.enums;

public enum StoreStatus {
    ACTIVE,
    PAUSED,
    CLOSED;

    public boolean isOperational() {
        return this == ACTIVE;
    }
}

