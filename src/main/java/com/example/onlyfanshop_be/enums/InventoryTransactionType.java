package com.example.onlyfanshop_be.enums;

/**
 * Loại giao dịch kho
 */
public enum InventoryTransactionType {
    TRANSFER_TO_STORE,   // Chuyển từ kho tổng sang kho cửa hàng
    RETURN_TO_CENTRAL,   // Trả hàng từ cửa hàng về kho tổng
    ADJUSTMENT,          // Điều chỉnh số lượng (kiểm kê)
    SALE,                // Bán hàng (trừ kho cửa hàng)
    IMPORT               // Nhập hàng vào kho tổng
}
