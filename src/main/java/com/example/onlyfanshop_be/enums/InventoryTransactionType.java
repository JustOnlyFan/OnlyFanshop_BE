package com.example.onlyfanshop_be.enums;

public enum InventoryTransactionType {
    TRANSFER,            // Chuyển hàng giữa các kho cửa hàng
    ADJUSTMENT,          // Điều chỉnh số lượng (kiểm kê)
    SALE,                // Bán hàng (trừ kho cửa hàng)
    IMPORT               // Nhập hàng vào kho cửa hàng
}
