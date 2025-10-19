package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.Order;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
@Service
public class OrderService implements IOrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Override
    public ApiResponse<List<OrderDTO>> getAllOrdersByUserID(int userId) {
        List<Order> listOrder = orderRepository.findOrdersByUser_UserID(userId);
        if (!listOrder.isEmpty()) {
            List<OrderDTO> listOrderDTO = new ArrayList<>();
            for (Order order : listOrder) {

                OrderDTO orderDTO = new OrderDTO();
                orderDTO.setOrderID(order.getOrderID());
                orderDTO.setOrderDate(order.getOrderDate());
                orderDTO.setOrderStatus(order.getOrderStatus());
                orderDTO.setBillingAddress(order.getBillingAddress());
                orderDTO.setPaymentMethod(order.getPaymentMethod());
                Cart cart = order.getCart();

                orderDTO.setTotalPrice(cart.getTotalPrice());
                listOrderDTO.add(orderDTO);
            }
            return ApiResponse.<List<OrderDTO>>builder().data(listOrderDTO).message("Tìm thấy danh sách order").statusCode(200).build();
        } else throw new AppException(ErrorCode.CART_NOTFOUND);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getAllOrders() {
        List<Order> listOrder = orderRepository.findAll();
        if (!listOrder.isEmpty()) {
            List<OrderDTO> listOrderDTO = new ArrayList<>();
            for (Order order : listOrder) {
                OrderDTO orderDTO = new OrderDTO();
                orderDTO.setOrderID(order.getOrderID());
                orderDTO.setOrderDate(order.getOrderDate());
                orderDTO.setOrderStatus(order.getOrderStatus());
                orderDTO.setBillingAddress(order.getBillingAddress());
                orderDTO.setPaymentMethod(order.getPaymentMethod());
                Cart cart = order.getCart();
                orderDTO.setTotalPrice(cart.getTotalPrice());
                listOrderDTO.add(orderDTO);
            }
            return ApiResponse.<List<OrderDTO>>builder().data(listOrderDTO).message("Tìm thấy danh sách order").statusCode(200).build();
        } else throw new AppException(ErrorCode.CART_NOTFOUND);
    }
    @Override
    public ApiResponse<OrderDetailsDTO> getOrderDetails(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        User user = order.getUser();
        Cart cart = order.getCart();

        CartDTO cartDTO = CartDTO.builder()
                .userId(user.getUserID())
                .items(cart.getCartItems())
                .totalQuantity(cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum())
                .build();

        return ApiResponse.<OrderDetailsDTO>builder().data( OrderDetailsDTO.builder()
                .orderID(order.getOrderID())
                .paymentMethod(order.getPaymentMethod())
                .billingAddress(order.getBillingAddress())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .totalPrice(cart.getTotalPrice())
                .address(user.getAddress())
                .customerName(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .cartDTO(cartDTO)
                .build()).build();


    }

}
