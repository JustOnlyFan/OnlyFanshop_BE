package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Cart;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.Order;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.OrderItemRepository;
import com.example.onlyfanshop_be.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderService implements IOrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private NotificationService notificationService;
    @Override
    public ApiResponse<List<OrderDTO>> getAllOrders(int userId, String status, String role) {
        List<Order> listOrder;

        // Nếu là admin thì lấy toàn bộ
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (status != null && !status.isEmpty()) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    listOrder = orderRepository.findOrdersByOrderStatus(orderStatus, Sort.by(Sort.Direction.DESC, "orderID"));
                } catch (IllegalArgumentException e) {
                    listOrder = orderRepository.findOrdersByOrderStatusString(status, Sort.by(Sort.Direction.DESC, "orderID"));
                }
            } else {
                listOrder = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderID"));
            }
        } else {
            // Nếu là user bình thường thì chỉ lấy theo userID
            if (status != null && !status.isEmpty()) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    listOrder = orderRepository.findOrdersByUser_UserIDAndOrderStatus(userId, orderStatus, Sort.by(Sort.Direction.DESC, "orderID"));
                } catch (IllegalArgumentException e) {
                    listOrder = orderRepository.findOrdersByUser_UserIDAndOrderStatusString(userId, status, Sort.by(Sort.Direction.DESC, "orderID"));
                }
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
            orderDTO.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
            orderDTO.setBillingAddress(order.getBillingAddress());
            orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            orderDTO.setTotalPrice(order.getTotalPrice());
            
            // Get first product info and all products from orderItems
            List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderID(order.getOrderID());
            if (orderItems != null && !orderItems.isEmpty()) {
                com.example.onlyfanshop_be.entity.OrderItem firstItem = orderItems.get(0);
                if (firstItem.getProduct() != null) {
                    orderDTO.setFirstProductName(firstItem.getProduct().getProductName());
                    orderDTO.setFirstProductImage(firstItem.getProduct().getImageURL());
                    orderDTO.setFirstProductQuantity(firstItem.getQuantity());
                    orderDTO.setFirstProductPrice(firstItem.getPrice());
                }
                
                // Build list of all products
                List<com.example.onlyfanshop_be.dto.OrderItemLiteDTO> productList = new ArrayList<>();
                int totalCount = 0;
                for (com.example.onlyfanshop_be.entity.OrderItem item : orderItems) {
                    com.example.onlyfanshop_be.dto.OrderItemLiteDTO lite = com.example.onlyfanshop_be.dto.OrderItemLiteDTO.builder()
                            .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                            .imageURL(item.getProduct() != null ? item.getProduct().getImageURL() : null)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build();
                    productList.add(lite);
                    totalCount += item.getQuantity() != null ? item.getQuantity() : 0;
                }
                orderDTO.setProducts(productList);
                orderDTO.setTotalProductCount(totalCount);
            }
            
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
                orderDTO.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
                orderDTO.setBillingAddress(order.getBillingAddress());
                orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
                orderDTO.setTotalPrice(order.getTotalPrice());
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

        // Lấy các item thuộc đơn hàng (đúng dữ liệu hiển thị chi tiết đơn)
        List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderID(order.getOrderID());
        CartDTO cartDTO = null;
        java.util.List<com.example.onlyfanshop_be.dto.OrderItemLiteDTO> lite = new java.util.ArrayList<>();
        if (orderItems != null && !orderItems.isEmpty()) {
            List<CartItem> mapped = new java.util.ArrayList<>();
            for (com.example.onlyfanshop_be.entity.OrderItem oi : orderItems) {
                CartItem ci = new CartItem();
                ci.setQuantity(oi.getQuantity() != null ? oi.getQuantity() : 0);
                ci.setPrice(oi.getPrice() != null ? oi.getPrice() : 0d);
                ci.setProduct(oi.getProduct());
                mapped.add(ci);

                // build lite dto
                com.example.onlyfanshop_be.dto.OrderItemLiteDTO l = com.example.onlyfanshop_be.dto.OrderItemLiteDTO.builder()
                        .productName(oi.getProduct() != null ? oi.getProduct().getProductName() : null)
                        .imageURL(oi.getProduct() != null ? oi.getProduct().getImageURL() : null)
                        .quantity(oi.getQuantity())
                        .price(oi.getPrice())
                        .build();
                lite.add(l);
            }
            int totalQty = mapped.stream().mapToInt(CartItem::getQuantity).sum();
            cartDTO = CartDTO.builder()
                    .userId(user.getUserID())
                    .items(mapped)
                    .totalQuantity(totalQty)
                    .build();
        }

        // Get shipping address from order, fallback to user address if not set
        String shippingAddress = order.getShippingAddress();
        if (shippingAddress == null || shippingAddress.isEmpty()) {
            shippingAddress = user.getAddress();
        }

        // Get recipient phone number from order, fallback to user phone if not set
        String recipientPhone = order.getRecipientPhoneNumber();
        if (recipientPhone == null || recipientPhone.isEmpty()) {
            recipientPhone = user.getPhoneNumber();
        }

        return ApiResponse.<OrderDetailsDTO>builder().data( OrderDetailsDTO.builder()
                .orderID(order.getOrderID())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .billingAddress(order.getBillingAddress())
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null)
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotalPrice())
                .address(shippingAddress)
                .customerName(user.getUsername())
                .email(user.getEmail())
                .phone(recipientPhone)
                .cartDTO(cartDTO)
                .itemsLite(lite)
                .build()).build();


    }
    @Override
    public ApiResponse<Void> setOrderStatus(int orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (status == null || status.trim().isEmpty()) {
            return ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Trạng thái đơn hàng không được để trống")
                    .build();
        }

        try {
            String statusUpper = status.toUpperCase().trim();
            OrderStatus orderStatus = OrderStatus.valueOf(statusUpper);
            order.setOrderStatus(orderStatus);
            orderRepository.save(order);

            // ✅ Gửi thông báo cho người dùng
            User user = order.getUser();
            if (user != null) {
                String message;

                switch (statusUpper) {
                    case "PICKING":
                        message = "Đơn hàng #" + orderId + " của bạn đã được duyệt và đang chờ lấy hàng!";
                        break;
                    case "SHIPPING":
                        message = "Đơn hàng #" + orderId + " của bạn đang được giao!";
                        break;
                    case "DELIVERED":
                        message = "Đơn hàng #" + orderId + " của bạn đã được giao thành công!";
                        break;
                    case "RETURNS_REFUNDS":
                        message = "Đơn hàng #" + orderId + " của bạn đang được xử lý hoàn trả/hoàn tiền.";
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
        } catch (IllegalArgumentException e) {
            return ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Trạng thái đơn hàng không hợp lệ: " + status + ". Các giá trị hợp lệ: " + 
                            String.join(", ", java.util.Arrays.stream(OrderStatus.values())
                                    .map(Enum::name)
                                    .toArray(String[]::new)))
                    .build();
        }
    }

    @Override
    public ApiResponse<Void> cancelOrder(int orderId, int userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND)); // Reuse error code

        // Check if user is owner of the order or is admin
        if (!"ADMIN".equalsIgnoreCase(role) && order.getUser().getUserID() != userId) {
            return ApiResponse.<Void>builder()
                    .statusCode(403)
                    .message("Bạn không có quyền hủy đơn hàng này")
                    .build();
        }

        // Check if order status is PENDING (chưa được approved)
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            return ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Chỉ có thể hủy đơn hàng khi đang ở trạng thái PENDING (chưa được duyệt)")
                    .build();
        }

        // Set order status to CANCELLED
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Hủy đơn hàng thành công")
                .build();
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersPending(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.PENDING, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersPicking(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.PICKING, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersShipping(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.SHIPPING, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersCompleted(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.DELIVERED, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersCancelled(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.CANCELLED, role);
    }

    private ApiResponse<List<OrderDTO>> getOrdersByStatus(int userId, OrderStatus status, String role) {
        List<Order> listOrder;

        // Nếu là admin thì lấy toàn bộ theo status
        if ("ADMIN".equalsIgnoreCase(role)) {
            listOrder = orderRepository.findOrdersByOrderStatus(status, Sort.by(Sort.Direction.DESC, "orderID"));
        } else {
            // Nếu là user bình thường thì chỉ lấy theo userID và status
            listOrder = orderRepository.findOrdersByUser_UserIDAndOrderStatus(userId, status, Sort.by(Sort.Direction.DESC, "orderID"));
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
            orderDTO.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
            orderDTO.setBillingAddress(order.getBillingAddress());
            orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            orderDTO.setTotalPrice(order.getTotalPrice());
            
            // Get first product info and all products from orderItems
            List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderID(order.getOrderID());
            if (orderItems != null && !orderItems.isEmpty()) {
                com.example.onlyfanshop_be.entity.OrderItem firstItem = orderItems.get(0);
                if (firstItem.getProduct() != null) {
                    orderDTO.setFirstProductName(firstItem.getProduct().getProductName());
                    orderDTO.setFirstProductImage(firstItem.getProduct().getImageURL());
                    orderDTO.setFirstProductQuantity(firstItem.getQuantity());
                    orderDTO.setFirstProductPrice(firstItem.getPrice());
                }
                
                // Build list of all products
                List<com.example.onlyfanshop_be.dto.OrderItemLiteDTO> productList = new ArrayList<>();
                int totalCount = 0;
                for (com.example.onlyfanshop_be.entity.OrderItem item : orderItems) {
                    com.example.onlyfanshop_be.dto.OrderItemLiteDTO lite = com.example.onlyfanshop_be.dto.OrderItemLiteDTO.builder()
                            .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                            .imageURL(item.getProduct() != null ? item.getProduct().getImageURL() : null)
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build();
                    productList.add(lite);
                    totalCount += item.getQuantity() != null ? item.getQuantity() : 0;
                }
                orderDTO.setProducts(productList);
                orderDTO.setTotalProductCount(totalCount);
            }
            
            listOrderDTO.add(orderDTO);
        }

        return ApiResponse.<List<OrderDTO>>builder()
                .data(listOrderDTO)
                .message("Tìm thấy danh sách order")
                .statusCode(200)
                .build();
    }

    @Override
    public ApiResponse<Void> deleteAllOrders() {
        try {
            // Get all orders
            List<Order> allOrders = orderRepository.findAll();
            
            // Delete all order items first
            for (Order order : allOrders) {
                orderItemRepository.deleteAll(orderItemRepository.findByOrder_OrderID(order.getOrderID()));
            }
            
            // Delete all orders
            orderRepository.deleteAll();
            
            return ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Đã xóa tất cả đơn hàng thành công")
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Void>builder()
                    .statusCode(500)
                    .message("Lỗi khi xóa đơn hàng: " + e.getMessage())
                    .build();
        }
    }
    @Override
    public Map<String, Long> countOrderBadgesByUser(int userId) {
        Map<String, Long> result = new HashMap<>();
        result.put("pending", orderRepository.countByUser_UserIDAndOrderStatus(userId, OrderStatus.PENDING));
        result.put("shipping", orderRepository.countByUser_UserIDAndOrderStatus(userId, OrderStatus.SHIPPING));
        result.put("picking", orderRepository.countByUser_UserIDAndOrderStatus(userId, OrderStatus.PICKING));
        return result;
    }
}
