package com.example.onlyfanshop_be.enums;

/**
 * Trạng thái yêu cầu chuyển kho
 */
public enum TransferRequestStatus {
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt
    REJECTED,   // Từ chối
    PARTIAL,    // Đáp ứng một phần (có debt order)
    COMPLETED   // Hoàn thành
}
