package com.example.onlyfanshop_be.enums;

/**
 * Trạng thái vận chuyển
 */
public enum ShipmentStatus {
    PENDING,        // Chờ lấy hàng
    READY_TO_PICK,  // Sẵn sàng lấy hàng
    PICKING,        // Đang lấy hàng
    PICKED,         // Đã lấy hàng
    STORING,        // Đang lưu kho
    IN_TRANSIT,     // Đang vận chuyển
    DELIVERING,     // Đang giao hàng
    DELIVERED,      // Đã giao hàng
    DELIVERY_FAIL,  // Giao hàng thất bại
    WAITING_TO_RETURN, // Chờ trả hàng
    RETURN,         // Đang trả hàng
    RETURNED,       // Đã trả hàng
    CANCEL,         // Đã hủy
    EXCEPTION       // Ngoại lệ
}
