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
import java.util.Map;

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

    @GetMapping("/getOrdersPicking")
    public ApiResponse<List<OrderDTO>> getOrdersPicking(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        return orderService.getOrdersPicking(userId, role);
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

    @DeleteMapping("/deleteAllOrders")
    public ApiResponse<Void> deleteAllOrders(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        String role = jwtTokenProvider.getRoleFromJWT(token);
        
        // Only admin can delete all orders
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.<Void>builder()
                    .statusCode(403)
                    .message("Chỉ admin mới có quyền xóa tất cả đơn hàng")
                    .build();
        }
        
        return orderService.deleteAllOrders();
    }
    @GetMapping("/badgeCount")
    public ApiResponse<Map<String, Long>> getOrderBadgeCount(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        Integer userId = jwtTokenProvider.getUserIdFromJWT(token);
        Map<String, Long> badgeCount = orderService.countOrderBadgesByUser(userId);

        return ApiResponse.<Map<String, Long>>builder()
                .statusCode(200)
                .message("Lấy số lượng đơn hàng thành công")
                .data(badgeCount)
                .build();
    }

}
