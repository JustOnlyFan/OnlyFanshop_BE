package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.config.VNPAYConfig;
import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.enums.PaymentMethod;
import com.example.onlyfanshop_be.enums.PaymentStatus;
import com.example.onlyfanshop_be.enums.PaymentTransactionStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartItemRepository;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.OrderItemRepository;
import com.example.onlyfanshop_be.repository.OrderRepository;
import com.example.onlyfanshop_be.repository.PaymentRepository;
import com.example.onlyfanshop_be.repository.UserAddressRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.NotificationService;
import com.example.onlyfanshop_be.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final VNPAYConfig vnPayConfig;
    @Autowired
    private final PaymentRepository paymentRepository;
    @Autowired
    private final CartRepository cartRepository;
    @Autowired
    private final OrderRepository orderRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserAddressRepository userAddressRepository;
    
    // Helper method to generate order code
    private String generateOrderCode(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + timestamp + userId;
    }
    
    // Helper method to create or find UserAddress
    private UserAddress getOrCreateUserAddress(User user, String address, String recipientPhone) {
        // Try to find default address first
        Optional<UserAddress> defaultAddressOpt = userAddressRepository.findByUserIdAndIsDefault(user.getId(), true);
        if (defaultAddressOpt.isPresent()) {
            return defaultAddressOpt.get();
        }
        
        // If address string is provided, try to parse and create
        if (address != null && !address.isEmpty()) {
            // For simplicity, create a new address with addressLine1 = address
            // In production, you might want to parse the address string more carefully
            UserAddress newAddress = UserAddress.builder()
                    .userId(user.getId())
                    .fullName(user.getFullname())
                    .phone(recipientPhone != null ? recipientPhone : (user.getPhone() != null ? user.getPhone() : ""))
                    .addressLine1(address)
                    .country("Vietnam")
                    .isDefault(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userAddressRepository.save(newAddress);
        }
        
        // If no address provided, create a default one
        UserAddress newAddress = UserAddress.builder()
                .userId(user.getId())
                .fullName(user.getFullname())
                .phone(user.getPhone() != null ? user.getPhone() : "")
                .addressLine1("")
                .country("Vietnam")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
        return userAddressRepository.save(newAddress);
    }
    
    // Helper method to calculate subtotal and total from cart items
    private BigDecimal[] calculateOrderTotals(List<CartItem> cartItems, BigDecimal shippingFee, BigDecimal discountTotal) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            if (item.getUnitPriceSnapshot() != null && item.getQuantity() != null) {
                BigDecimal itemTotal = item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(itemTotal);
            }
        }
        BigDecimal totalAmount = subtotal.add(shippingFee != null ? shippingFee : BigDecimal.ZERO)
                .subtract(discountTotal != null ? discountTotal : BigDecimal.ZERO);
        return new BigDecimal[]{subtotal, totalAmount};
    }

    @GetMapping("/vn-pay")
    public ApiResponse<PaymentDTO.VNPayResponse> pay(
            HttpServletRequest request,
            @RequestParam Double amount,
            @RequestParam String bankCode,
            @RequestParam String address,
            @RequestParam String buyMethod,
            @RequestParam(required = false) String recipientPhoneNumber,
            @RequestParam(required = false, defaultValue = "web") String clientType
    ) {
        String token = jwtTokenProvider.extractToken(request);
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        Cart cart;
        cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));

        PaymentDTO.VNPayResponse responseData = paymentService.createVnPayPayment(
                request, amount, bankCode, cart.getId().intValue(), address, recipientPhoneNumber, clientType);

        return ApiResponse.<PaymentDTO.VNPayResponse>builder()
                .statusCode(200)
                .message("Tạo thanh toán thành công")
                .data(responseData)
                .build();
    }

    @PostMapping("/cod")
    public ApiResponse<Integer> createCODOrder(
            HttpServletRequest request,
            @RequestParam Double totalPrice,
            @RequestParam String address,
            @RequestParam String buyMethod,
            @RequestParam(required = false) String recipientPhoneNumber,
            @RequestParam(required = false) String deliveryType,
            @RequestParam(required = false) Integer storeId
    ) {
        try {
            String token = jwtTokenProvider.extractToken(request);
            Long userId = jwtTokenProvider.getUserIdFromJWT(token);

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

            UserAddress userAddress = getOrCreateUserAddress(user, address, recipientPhoneNumber);

            List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
            if (cartItems.isEmpty()) {
                throw new AppException(ErrorCode.CART_NOTFOUND);
            }

            BigDecimal shippingFee = BigDecimal.ZERO; // Can be calculated based on delivery type
            BigDecimal discountTotal = BigDecimal.ZERO;
            BigDecimal[] totals = calculateOrderTotals(cartItems, shippingFee, discountTotal);
            BigDecimal subtotal = totals[0];
            BigDecimal totalAmount = totals[1];

            Order order = Order.builder()
                    .userId(user.getId())
                    .addressId(userAddress.getId())
                    .orderCode(generateOrderCode(user.getId()))
                    .status(OrderStatus.pending)
                    .paymentMethod(PaymentMethod.cod)
                    .paymentStatus(PaymentStatus.unpaid)
                    .shippingMethod(deliveryType != null ? deliveryType : "HOME_DELIVERY")
                    .shippingFee(shippingFee)
                    .discountTotal(discountTotal)
                    .subtotal(subtotal)
                    .totalAmount(totalAmount)
                    .createdAt(LocalDateTime.now())
                    .build();
            order = orderRepository.save(order);

            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                if (product == null) {
                    continue; // Skip if product is null
                }
                
                BigDecimal unitPrice = cartItem.getUnitPriceSnapshot() != null ? 
                        cartItem.getUnitPriceSnapshot() : BigDecimal.ZERO;
                Integer quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                
                OrderItem orderItem = OrderItem.builder()
                        .orderId(order.getId())
                        .productId(cartItem.getProductId())
                        .productName(product.getName() != null ? product.getName() : "")
                        .sku(product.getSku())
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .lineTotal(lineTotal)
                        .build();
                orderItemRepository.save(orderItem);
            }

            cartItemRepository.deleteAll(cartItems);
            cartRepository.delete(cart);

            notificationService.sendNotification(
                    user.getId().intValue(),
                    "Đơn hàng #" + order.getOrderCode() + " đã được tạo thành công! Chờ xác nhận."
            );

            return ApiResponse.<Integer>builder()
                    .statusCode(200)
                    .message("Tạo đơn hàng COD thành công")
                    .data(order.getId().intValue())
                    .build();

        } catch (AppException e) {
            return ApiResponse.<Integer>builder()
                    .statusCode(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        } catch (Exception e) {
            return ApiResponse.<Integer>builder()
                    .statusCode(500)
                    .message("Lỗi server: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/public/vn-pay-callback")
    public void vnPayCallback(@RequestParam Map<String, String> params,
                              @RequestParam(required = false) String address,
                              @RequestParam(required = false) String recipientPhoneNumber,
                              @RequestParam(required = false, defaultValue = "web") String clientType,
                              HttpServletResponse response) throws IOException {

        String responseCode = params.get("vnp_ResponseCode");
        String paymentCode = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");
        String cardIdStr = params.get("vnp_TxnRef").split("_")[0];

        boolean exists = paymentRepository.existsByProviderTxnId(paymentCode);
        if (exists) return;

        BigDecimal amount = amountStr != null ? 
                BigDecimal.valueOf(Double.parseDouble(amountStr) / 100) : BigDecimal.ZERO;
        
        Payment payment = Payment.builder()
                .providerTxnId(paymentCode)
                .amount(amount)
                .method(PaymentMethod.online_gateway)
                .status(PaymentTransactionStatus.pending)
                .createdAt(LocalDateTime.now())
                .build();
        
        if ("00".equals(responseCode)) {
            // Giao dịch thành công
            Cart cart = cartRepository.findById(Long.parseLong(cardIdStr))
                    .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
            
            User user = userRepository.findById(cart.getUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

            // Get all cart items (no status field in new schema)
            List<CartItem> cartItemsOrder = cartItemRepository.findByCartId(cart.getId());

            // Tạo hoặc lấy UserAddress
            UserAddress userAddress = getOrCreateUserAddress(user, address, recipientPhoneNumber);
            
            // Tính toán subtotal và total
            BigDecimal shippingFee = BigDecimal.ZERO;
            BigDecimal discountTotal = BigDecimal.ZERO;
            BigDecimal[] totals = calculateOrderTotals(cartItemsOrder, shippingFee, discountTotal);
            BigDecimal subtotal = totals[0];
            BigDecimal totalAmount = totals[1];

            // Tạo order
            Order order = Order.builder()
                    .userId(user.getId())
                    .addressId(userAddress.getId())
                    .orderCode(generateOrderCode(user.getId()))
                    .status(OrderStatus.confirmed)
                    .paymentMethod(PaymentMethod.online_gateway)
                    .paymentStatus(PaymentStatus.paid)
                    .shippingMethod("HOME_DELIVERY")
                    .shippingFee(shippingFee)
                    .discountTotal(discountTotal)
                    .subtotal(subtotal)
                    .totalAmount(totalAmount)
                    .createdAt(LocalDateTime.now())
                    .confirmedAt(LocalDateTime.now())
                    .build();
            order = orderRepository.save(order);
            
            // Tạo order items từ cart items
            for (CartItem cartItem : cartItemsOrder) {
                Product product = cartItem.getProduct();
                if (product == null) {
                    continue; // Skip if product is null
                }
                
                BigDecimal unitPrice = cartItem.getUnitPriceSnapshot() != null ? 
                        cartItem.getUnitPriceSnapshot() : BigDecimal.ZERO;
                Integer quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
                
                OrderItem orderItem = OrderItem.builder()
                        .orderId(order.getId())
                        .productId(cartItem.getProductId())
                        .productName(product.getName() != null ? product.getName() : "")
                        .sku(product.getSku())
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .lineTotal(lineTotal)
                        .build();
                orderItemRepository.save(orderItem);
            }
            
            // Xóa cart items và cart
            cartItemRepository.deleteAll(cartItemsOrder);
            cartRepository.delete(cart);

            // Cập nhật payment
            payment.setOrderId(order.getId());
            payment.setStatus(PaymentTransactionStatus.success);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Gọi service gửi thông báo (tự động lưu DB + đẩy Firebase)
            notificationService.sendNotification(
                    user.getId().intValue(),
                    "Thanh toán thành công đơn hàng #" + order.getOrderCode()
            );

            // Redirect theo client type
            String redirectUrl;
            if ("web".equalsIgnoreCase(clientType)) {
                redirectUrl = vnPayConfig.getWebBaseUrl() + "/payment-result?status=success&code=" + paymentCode + "&order=" + order.getId();
            } else {
                redirectUrl = vnPayConfig.getAppDeepLink() + "/payment-result?status=success&code=" + paymentCode + "&order=" + order.getId();
            }
            response.sendRedirect(redirectUrl);

        } else {
            // Giao dịch thất bại
            payment.setStatus(PaymentTransactionStatus.failed);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Redirect theo client type
            String redirectUrl;
            if ("web".equalsIgnoreCase(clientType)) {
                redirectUrl = vnPayConfig.getWebBaseUrl() + "/payment-result?status=fail&code=" + paymentCode;
            } else {
                redirectUrl = vnPayConfig.getAppDeepLink() + "/payment-result?status=fail&code=" + paymentCode;
            }
            response.sendRedirect(redirectUrl);
        }
    }


}

