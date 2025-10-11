package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private int totalQuantity; // Tổng số lượng item trong giỏ
    private List<CartItem> items;
    private int userId;// Danh sách chi tiết từng item
}
