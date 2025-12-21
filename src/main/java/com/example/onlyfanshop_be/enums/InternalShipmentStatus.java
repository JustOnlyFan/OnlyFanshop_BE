package com.example.onlyfanshop_be.enums;

/**
 * Trạng thái vận chuyển nội bộ giữa các kho
 */
public enum InternalShipmentStatus {
    CREATED,        // Đã tạo đơn
    PICKING,        // Đang lấy hàng
    PICKED,         // Đã lấy hàng
    IN_TRANSIT,     // Đang vận chuyển
    DELIVERING,     // Đang giao hàng
    DELIVERED,      // Đã giao
    CANCELLED,      // Đã hủy
    RETURN          // Hoàn trả
}
