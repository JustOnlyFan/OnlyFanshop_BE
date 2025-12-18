package com.example.onlyfanshop_be.enums;

/**
 * Đơn vị vận chuyển
 */
public enum ShippingCarrier {
    GHN("Giao Hàng Nhanh"),
    GHTK("Giao Hàng Tiết Kiệm"),
    VIETTEL_POST("Viettel Post"),
    VNPOST("Vietnam Post"),
    JT_EXPRESS("J&T Express"),
    BEST_EXPRESS("Best Express"),
    NINJA_VAN("Ninja Van"),
    OTHER("Khác");

    private final String displayName;

    ShippingCarrier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
