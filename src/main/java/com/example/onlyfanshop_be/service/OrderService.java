package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.CartDTO;
import com.example.onlyfanshop_be.dto.OrderDTO;
import com.example.onlyfanshop_be.dto.OrderDetailsDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.CartItem;
import com.example.onlyfanshop_be.entity.Order;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.entity.UserAddress;
import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
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
                    // OrderStatus enum values are lowercase
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toLowerCase());
                    listOrder = orderRepository.findOrdersByStatus(orderStatus, Sort.by(Sort.Direction.DESC, "createdAt"));
                } catch (IllegalArgumentException e) {
                    // If status doesn't match enum, return empty list
                    listOrder = Collections.emptyList();
                }
            } else {
                listOrder = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        } else {
            // Nếu là user bình thường thì chỉ lấy theo userID
            Long userIdLong = (long) userId;
            if (status != null && !status.isEmpty()) {
                try {
                    // OrderStatus enum values are lowercase
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toLowerCase());
                    listOrder = orderRepository.findOrdersByUserIdAndStatus(userIdLong, orderStatus, Sort.by(Sort.Direction.DESC, "createdAt"));
                } catch (IllegalArgumentException e) {
                    // If status doesn't match enum, return empty list
                    listOrder = Collections.emptyList();
                }
            } else {
                listOrder = orderRepository.findByUserId(userIdLong, Sort.by(Sort.Direction.DESC, "createdAt"));
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
            orderDTO.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
            orderDTO.setBillingAddress(getBillingAddressString(order));
            orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            orderDTO.setTotalPrice(order.getTotalPrice());
            
            // Get first product info and all products from orderItems
            List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderId(order.getId());
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
                orderDTO.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
                orderDTO.setBillingAddress(getBillingAddressString(order));
                orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
                orderDTO.setTotalPrice(order.getTotalPrice());
                listOrderDTO.add(orderDTO);
            }
            return ApiResponse.<List<OrderDTO>>builder().data(listOrderDTO).message("Tìm thấy danh sách order").statusCode(200).build();
        } else throw new AppException(ErrorCode.CART_NOTFOUND);
    }
    @Override
    public ApiResponse<OrderDetailsDTO> getOrderDetails(int orderId) {
        Order order = orderRepository.findById((long) orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        User user = order.getUser();

        // Lấy các item thuộc đơn hàng (đúng dữ liệu hiển thị chi tiết đơn)
        List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderId(order.getId());
        CartDTO cartDTO = null;
        java.util.List<com.example.onlyfanshop_be.dto.OrderItemLiteDTO> lite = new java.util.ArrayList<>();
        if (orderItems != null && !orderItems.isEmpty()) {
            List<CartItem> mapped = new java.util.ArrayList<>();
            for (com.example.onlyfanshop_be.entity.OrderItem oi : orderItems) {
                CartItem ci = new CartItem();
                ci.setQuantity(oi.getQuantity() != null ? oi.getQuantity() : 0);
                // Note: CartItem doesn't have setPrice, use unitPriceSnapshot
                if (oi.getUnitPrice() != null) {
                    ci.setUnitPriceSnapshot(oi.getUnitPrice());
                }
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
                    .userId(user.getId().intValue())
                    .items(mapped)
                    .totalQuantity(totalQty)
                    .build();
        }

        // Get shipping address from order UserAddress, fallback to user address if not set
        String shippingAddress = getShippingAddressString(order);
        if (shippingAddress == null || shippingAddress.isEmpty()) {
            shippingAddress = user.getAddress();
        }

        // Get recipient phone number from order UserAddress, fallback to user phone if not set
        String recipientPhone = getRecipientPhoneString(order);
        if (recipientPhone == null || recipientPhone.isEmpty()) {
            recipientPhone = user.getPhoneNumber();
        }

        return ApiResponse.<OrderDetailsDTO>builder().data( OrderDetailsDTO.builder()
                .orderID(order.getOrderID())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .billingAddress(getBillingAddressString(order))
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotalPrice())
                .address(shippingAddress)
                .customerName(user.getFullname())
                .email(user.getEmail())
                .phone(recipientPhone)
                .cartDTO(cartDTO)
                .itemsLite(lite)
                .build()).build();


    }
    @Override
    public ApiResponse<Void> setOrderStatus(int orderId, String status) {
        Order order = orderRepository.findById((long) orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (status == null || status.trim().isEmpty()) {
            return ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Trạng thái đơn hàng không được để trống")
                    .build();
        }

        try {

            String statusLower = status.toLowerCase().trim();
            OrderStatus orderStatus = OrderStatus.valueOf(statusLower);
            order.setStatus(orderStatus);
            orderRepository.save(order);

            User user = order.getUser();
            if (user != null) {
                String message;

                switch (statusLower) {
                    case "processing":
                        message = "Đơn hàng #" + orderId + " của bạn đã được duyệt và đang chờ lấy hàng!";
                        break;
                    case "shipping":
                        message = "Đơn hàng #" + orderId + " của bạn đang được giao!";
                        break;
                    case "completed":
                        message = "Đơn hàng #" + orderId + " của bạn đã được giao thành công!";
                        break;
                    case "refunded":
                        message = "Đơn hàng #" + orderId + " của bạn đang được xử lý hoàn trả/hoàn tiền.";
                        break;
                    case "canceled":
                        message = "Đơn hàng #" + orderId + " của bạn đã bị hủy.";
                        break;
                    default:
                        message = "Trạng thái đơn hàng #" + orderId + " đã được cập nhật: " + status;
                        break;
                }

                notificationService.sendNotification(user.getId().intValue(), message);
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
        Order order = orderRepository.findById((long) orderId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND)); // Reuse error code

        // Check if user is owner of the order or is admin
        if (!"ADMIN".equalsIgnoreCase(role) && !order.getUser().getId().equals((long) userId)) {
            return ApiResponse.<Void>builder()
                    .statusCode(403)
                    .message("Bạn không có quyền hủy đơn hàng này")
                    .build();
        }

        // Check if order status is pending (chưa được approved)
        if (order.getStatus() != OrderStatus.pending) {
            return ApiResponse.<Void>builder()
                    .statusCode(400)
                    .message("Chỉ có thể hủy đơn hàng khi đang ở trạng thái pending (chưa được duyệt)")
                    .build();
        }

        // Set order status to canceled
        order.setStatus(OrderStatus.canceled);
        order.setCanceledAt(java.time.LocalDateTime.now());
        orderRepository.save(order);

        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Hủy đơn hàng thành công")
                .build();
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersPending(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.pending, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersPicking(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.processing, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersShipping(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.shipping, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersCompleted(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.completed, role);
    }

    @Override
    public ApiResponse<List<OrderDTO>> getOrdersCancelled(int userId, String role) {
        return getOrdersByStatus(userId, OrderStatus.canceled, role);
    }

    private ApiResponse<List<OrderDTO>> getOrdersByStatus(int userId, OrderStatus status, String role) {
        List<Order> listOrder;

        // Nếu là admin thì lấy toàn bộ theo status
        if ("ADMIN".equalsIgnoreCase(role)) {
            listOrder = orderRepository.findOrdersByStatus(status, Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            // Nếu là user bình thường thì chỉ lấy theo userID và status
            Long userIdLong = (long) userId;
            listOrder = orderRepository.findOrdersByUserIdAndStatus(userIdLong, status, Sort.by(Sort.Direction.DESC, "createdAt"));
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
            orderDTO.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
            orderDTO.setBillingAddress(getBillingAddressString(order));
            orderDTO.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
            orderDTO.setTotalPrice(order.getTotalPrice());
            
            // Get first product info and all products from orderItems
            List<com.example.onlyfanshop_be.entity.OrderItem> orderItems = orderItemRepository.findOrderItemsByOrderId(order.getId());
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
                orderItemRepository.deleteAll(orderItemRepository.findByOrderId(order.getId()));
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
        Long userIdLong = (long) userId;
        result.put("pending", orderRepository.countByUserIdAndStatus(userIdLong, OrderStatus.pending));
        result.put("shipping", orderRepository.countByUserIdAndStatus(userIdLong, OrderStatus.shipping));
        result.put("picking", orderRepository.countByUserIdAndStatus(userIdLong, OrderStatus.processing));
        return result;
    }

    // Helper method to get billing address as string from Order
    private String getBillingAddressString(Order order) {
        if (order.getAddress() != null) {
            UserAddress address = order.getAddress();
            StringBuilder sb = new StringBuilder();
            if (address.getAddressLine1() != null) {
                sb.append(address.getAddressLine1());
            }
            if (address.getAddressLine2() != null && !address.getAddressLine2().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getAddressLine2());
            }
            if (address.getWard() != null && !address.getWard().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getWard());
            }
            if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getDistrict());
            }
            if (address.getCity() != null && !address.getCity().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getCity());
            }
            if (address.getCountry() != null && !address.getCountry().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getCountry());
            }
            return sb.toString();
        }
        return null;
    }

    // Helper method to get shipping address as string from Order
    private String getShippingAddressString(Order order) {
        return getBillingAddressString(order); // Same as billing address in new schema
    }

    // Helper method to get recipient phone as string from Order
    private String getRecipientPhoneString(Order order) {
        if (order.getAddress() != null && order.getAddress().getPhone() != null) {
            return order.getAddress().getPhone();
        }
        return null;
    }
}
