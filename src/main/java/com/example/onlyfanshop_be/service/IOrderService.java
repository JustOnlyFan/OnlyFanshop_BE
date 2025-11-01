package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Order;

import java.util.List;

public interface IOrderService {
    public ApiResponse<List<OrderDTO>> getAllOrders(int userId, String status, String role);
    public ApiResponse<List<OrderDTO>>  getAllOrders();
    public ApiResponse<OrderDetailsDTO> getOrderDetails(int orderId);
    public ApiResponse<Void> setOrderStatus(int orderId, String status);
}
