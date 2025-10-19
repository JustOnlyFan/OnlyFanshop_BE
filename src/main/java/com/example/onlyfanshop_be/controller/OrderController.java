package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.IOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private IOrderService orderService;
    @GetMapping("/getOrders")
    public ApiResponse<List<OrderDTO>> getOrders(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        return orderService.getAllOrdersByUserID(userid);
    }

    @GetMapping("/getAllOrders")
    public ApiResponse<List<OrderDTO>> getAllOrders(HttpServletRequest request) {
        return orderService.getAllOrders();
    }

    @GetMapping("/getOrderDetails")
    public ApiResponse<OrderDetailsDTO> getOrderDetail(@RequestParam int orderId) {
        return orderService.getOrderDetails(orderId);
    }
}
