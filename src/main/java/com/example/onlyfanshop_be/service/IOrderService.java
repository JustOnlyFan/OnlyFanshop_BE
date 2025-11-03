package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

import java.util.List;

public interface IOrderService {
    public ApiResponse<List<OrderDTO>> getAllOrders(int userId, String status, String role);
    public ApiResponse<List<OrderDTO>>  getAllOrders();
    public ApiResponse<OrderDetailsDTO> getOrderDetails(int orderId);
    public ApiResponse<Void> setOrderStatus(int orderId, String status);
    public ApiResponse<Void> cancelOrder(int orderId, int userId, String role);
    public ApiResponse<List<OrderDTO>> getOrdersPending(int userId, String role);
    public ApiResponse<List<OrderDTO>> getOrdersPicking(int userId, String role);
    public ApiResponse<List<OrderDTO>> getOrdersShipping(int userId, String role);
    public ApiResponse<List<OrderDTO>> getOrdersCompleted(int userId, String role);
    public ApiResponse<Void> deleteAllOrders();
}
