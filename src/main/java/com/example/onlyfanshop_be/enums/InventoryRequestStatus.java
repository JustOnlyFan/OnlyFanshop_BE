package com.example.onlyfanshop_be.enums;

/**
 * Trạng thái của yêu cầu nhập hàng từ cửa hàng
 * Flow: PENDING → APPROVED → SHIPPING → DELIVERED
 */
public enum InventoryRequestStatus {
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt (chờ vận chuyển)
    SHIPPING,   // Đang vận chuyển đến kho
    DELIVERED,  // Đã giao hàng - cập nhật số lượng kho
    REJECTED,   // Từ chối
    CANCELLED   // Đã hủy bởi cửa hàng
}
