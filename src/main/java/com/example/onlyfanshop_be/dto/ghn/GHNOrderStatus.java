package com.example.onlyfanshop_be.dto.ghn;

/**
 * GHN Order Status - Trạng thái đơn hàng GHN
 * Mapping từ GHN API status codes
 */
public enum GHNOrderStatus {
    READY_TO_PICK("ready_to_pick", "Chờ lấy hàng"),
    PICKING("picking", "Đang lấy hàng"),
    PICKED("picked", "Đã lấy hàng"),
    STORING("storing", "Đang lưu kho"),
    TRANSPORTING("transporting", "Đang vận chuyển"),
    SORTING("sorting", "Đang phân loại"),
    DELIVERING("delivering", "Đang giao hàng"),
    DELIVERED("delivered", "Đã giao hàng"),
    DELIVERY_FAIL("delivery_fail", "Giao hàng thất bại"),
    WAITING_TO_RETURN("waiting_to_return", "Chờ hoàn hàng"),
    RETURN("return", "Đang hoàn hàng"),
    RETURN_TRANSPORTING("return_transporting", "Đang vận chuyển hoàn"),
    RETURN_SORTING("return_sorting", "Đang phân loại hoàn"),
    RETURNING("returning", "Đang hoàn hàng"),
    RETURN_FAIL("return_fail", "Hoàn hàng thất bại"),
    RETURNED("returned", "Đã hoàn hàng"),
    CANCEL("cancel", "Đã hủy"),
    EXCEPTION("exception", "Ngoại lệ"),
    LOST("lost", "Thất lạc"),
    DAMAGE("damage", "Hư hỏng");

    private final String code;
    private final String description;

    GHNOrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse status from GHN API response
     */
    public static GHNOrderStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (GHNOrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return EXCEPTION;
    }

    /**
     * Check if order is in final state
     */
    public boolean isFinalState() {
        return this == DELIVERED || this == RETURNED || this == CANCEL || this == LOST || this == DAMAGE;
    }

    /**
     * Check if order is successfully delivered
     */
    public boolean isDelivered() {
        return this == DELIVERED;
    }

    /**
     * Check if order is cancelled or returned
     */
    public boolean isCancelledOrReturned() {
        return this == CANCEL || this == RETURNED || this == RETURN_FAIL;
    }
}
