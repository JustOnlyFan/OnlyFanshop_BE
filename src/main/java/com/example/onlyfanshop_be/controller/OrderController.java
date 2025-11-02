package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.IOrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private IOrderService orderService;

    @GetMapping("/getOrders")
    public ApiResponse<List<OrderDTO>> getOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {

        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getAllOrders(userId, status, role);
    }

    @GetMapping("/getAllOrders")
    public ApiResponse<List<OrderDTO>> getAllOrders(HttpServletRequest request) {
        return orderService.getAllOrders();
    }



    @GetMapping("/getOrderDetails")
    public ApiResponse<OrderDetailsDTO> getOrderDetail(@RequestParam int orderId) {
        return orderService.getOrderDetails(orderId);
    }
    @PutMapping("/setOrderStatus")
    public ApiResponse<Void> setOrderStatus(@RequestParam int orderId, @RequestParam String status) {
        return orderService.setOrderStatus(orderId, status);
    }

    @PutMapping("/cancelOrder")
    public ApiResponse<Void> cancelOrder(
            HttpServletRequest request,
            @RequestParam int orderId
    ) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.cancelOrder(orderId, userId, role);
    }

    @GetMapping("/getOrdersPending")
    public ApiResponse<List<OrderDTO>> getOrdersPending(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getOrdersPending(userId, role);
    }

    @GetMapping("/getOrdersConfirmed")
    public ApiResponse<List<OrderDTO>> getOrdersConfirmed(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getOrdersConfirmed(userId, role);
    }

    @GetMapping("/getOrdersShipping")
    public ApiResponse<List<OrderDTO>> getOrdersShipping(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getOrdersShipping(userId, role);
    }

    @GetMapping("/getOrdersCompleted")
    public ApiResponse<List<OrderDTO>> getOrdersCompleted(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getOrdersCompleted(userId, role);
    }
}
