package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Order;

import java.util.List;

public interface IOrderService {
    public ApiResponse<List<OrderDTO>> getAllOrdersByUserID(int userId);
    public ApiResponse<List<OrderDTO>>  getAllOrders();
    public ApiResponse<OrderDetailsDTO> getOrderDetails(int orderId);
}
