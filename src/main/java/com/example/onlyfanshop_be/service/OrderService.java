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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Service
public class OrderService implements IOrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;
    @Override
    public ApiResponse<List<OrderDTO>> getAllOrders(int userId, String status, String role) {
        List<Order> listOrder;

        // Nếu là admin thì lấy toàn bộ
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (status != null && !status.isEmpty()) {
                listOrder = orderRepository.findOrdersByOrderStatus(status, Sort.by(Sort.Direction.DESC, "orderID"));
            } else {
                listOrder = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderID"));
            }
        } else {
            // Nếu là user bình thường thì chỉ lấy theo userID
            if (status != null && !status.isEmpty()) {
                listOrder = orderRepository.findOrdersByUser_UserIDAndOrderStatus(userId, status, Sort.by(Sort.Direction.DESC, "orderID"));
            } else {
                listOrder = orderRepository.findOrdersByUser_UserID(userId, Sort.by(Sort.Direction.DESC, "orderID"));
            }
        }

        if (listOrder.isEmpty()) {
            return ApiResponse.<List<OrderDTO>>builder()
                    .statusCode(200)
                    .message("Không có đơn hàng nào")
                    .data(Collections.emptyList())
                    .build();
        }

        List<OrderDTO> listOrderDTO = new ArrayList<>();
        for (Order order : listOrder) {
            OrderDTO orderDTO = new OrderDTO();
            orderDTO.setOrderID(order.getOrderID());
            orderDTO.setOrderDate(order.getOrderDate());
            orderDTO.setOrderStatus(order.getOrderStatus());
            orderDTO.setBillingAddress(order.getBillingAddress());
            orderDTO.setPaymentMethod(order.getPaymentMethod());
            //orderDTO.setTotalPrice(order.getCart().getTotalPrice());
            listOrderDTO.add(orderDTO);
        }

        return ApiResponse.<List<OrderDTO>>builder()
                .data(listOrderDTO)
                .message("Tìm thấy danh sách order")
                .statusCode(200)
                .build();
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
                //Cart cart = order.getCart();
                //orderDTO.setTotalPrice(cart.getTotalPrice());
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
        //Cart cart = order.getCart();

        CartDTO cartDTO = CartDTO.builder()
                .userId(user.getUserID())
                //.items(cart.getCartItems())
                //.totalQuantity(cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum())
                .build();

        return ApiResponse.<OrderDetailsDTO>builder().data( OrderDetailsDTO.builder()
                .orderID(order.getOrderID())
                .paymentMethod(order.getPaymentMethod())
                .billingAddress(order.getBillingAddress())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                //.totalPrice(cart.getTotalPrice())
                .address(user.getAddress())
                .customerName(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .cartDTO(cartDTO)
                .build()).build();


    }
    @Override
    public ApiResponse<Void> setOrderStatus(int orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setOrderStatus(status);
        orderRepository.save(order);

        // ✅ Gửi thông báo cho người dùng
        User user = order.getUser();
        if (user != null) {
            String message;

            switch (status.toUpperCase()) {
                case "CONFIRMED":
                    message = "Đơn hàng #" + orderId + " của bạn đã được xác nhận!";
                    break;
                case "SHIPPING":
                    message = "Đơn hàng #" + orderId + " của bạn đang được giao!";
                    break;
                case "COMPLETED":
                    message = "Đơn hàng #" + orderId + " của bạn đã hoàn tất. Cảm ơn bạn đã mua hàng!";
                    break;
                case "CANCELLED":
                    message = "Đơn hàng #" + orderId + " của bạn đã bị hủy.";
                    break;
                default:
                    message = "Trạng thái đơn hàng #" + orderId + " đã được cập nhật: " + status;
                    break;
            }

            // ✅ Gọi service gửi thông báo
            notificationService.sendNotification(user.getUserID(), message);
        }

        return ApiResponse.<Void>builder()
                .message("Cập nhật thành công")
                .statusCode(200)
                .build();
    }

}
