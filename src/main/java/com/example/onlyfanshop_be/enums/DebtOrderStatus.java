package com.example.onlyfanshop_be.enums;

/**
 * Trạng thái đơn nợ
 */
public enum DebtOrderStatus {
    PENDING,     // Chờ xử lý
    FULFILLABLE, // Có thể đáp ứng
    COMPLETED    // Đã hoàn thành
}
