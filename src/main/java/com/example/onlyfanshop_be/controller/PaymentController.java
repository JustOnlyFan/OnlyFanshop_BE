package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.config.VNPAYConfig;
import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.DeliveryType;
import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.enums.PaymentMethod;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.*;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private StoreLocationRepository storeLocationRepository;
    @Autowired
    private UserRepository userRepository;
    //    @GetMapping("/vn-pay")
//    public ApiResponse<PaymentDTO.VNPayResponse> pay(HttpServletRequest request, @RequestParam Double amount, @RequestParam String bankCode,@RequestParam int cardId) {
//        return ApiResponse.<PaymentDTO.VNPayResponse>builder().statusCode(200).message("Thanh cong").data(paymentService.createVnPayPayment(request,amount,bankCode, cardId)).build();
//    }
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
        // ✅ 1. Lấy token từ header
        String token = jwtTokenProvider.extractToken(request);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        Cart cart;
        // ✅ 3. Lấy cart tương ứng với user
        if(buyMethod.equals("Instant")) {
            cart= cartRepository.findByUser_UserIDAndStatus(userid, "InstantBuy").orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
            cart.setStatus("Pending");
            cartRepository.save(cart);
        }else if (buyMethod.equals("ByCart")){
            cart = cartRepository.findByUser_UserIDAndStatus(userid, "InProgress")
                    .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
        } else{
            throw new AppException(ErrorCode.BUY_METHOD_INVALID);
        }


        // ✅ 4. Gọi service xử lý thanh toán
        PaymentDTO.VNPayResponse responseData = paymentService.createVnPayPayment(request, amount, bankCode, cart.getCartID(), address, recipientPhoneNumber, clientType);

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
            // ✅ 1. Lấy token từ header
            String token = jwtTokenProvider.extractToken(request);
            int userid = jwtTokenProvider.getUserIdFromJWT(token);
            
            // ✅ 2. Lấy cart tương ứng với user
            Cart cart;
            if (buyMethod.equals("Instant")) {
                cart = cartRepository.findByUser_UserIDAndStatus(userid, "InstantBuy")
                        .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
                cart.setStatus("Pending");
                cartRepository.save(cart);
            } else if (buyMethod.equals("ByCart")) {
                cart = cartRepository.findByUser_UserIDAndStatus(userid, "InProgress")
                        .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
            } else {
                throw new AppException(ErrorCode.BUY_METHOD_INVALID);
            }

            // ✅ 3. Lấy user
            User user = userRepository.findById(userid)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

            // ✅ 4. Tạo order
            Order order = new Order();
            order.setUser(user);
            order.setTotalPrice(totalPrice);
            order.setBillingAddress(
                    (address != null && !address.isEmpty()) ? address : user.getAddress()
            );
            order.setOrderStatus(OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());
            order.setPaymentMethod(PaymentMethod.COD);
            
            // ✅ 5. Set delivery type và address
            if (deliveryType != null && deliveryType.equalsIgnoreCase("IN_STORE_PICKUP") && storeId != null) {
                order.setDeliveryType(DeliveryType.IN_STORE_PICKUP);
                StoreLocation store = storeLocationRepository.findById(storeId)
                        .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
                order.setPickupStore(store);
                // Use store address as shipping address
                order.setShippingAddress(store.getAddress());
            } else {
                order.setDeliveryType(DeliveryType.HOME_DELIVERY);
                // Set shipping address from parameter, fallback to user address if not provided
                if (address != null && !address.isEmpty()) {
                    order.setShippingAddress(address);
                } else if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                    order.setShippingAddress(user.getAddress());
                }
            }

            // ✅ 6. Set recipient phone number
            if (recipientPhoneNumber != null && !recipientPhoneNumber.isEmpty()) {
                order.setRecipientPhoneNumber(recipientPhoneNumber);
            } else if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                order.setRecipientPhoneNumber(user.getPhoneNumber());
            }

            // ✅ 7. Lưu order
            order = orderRepository.save(order);

            // ✅ 8. Tạo order items từ cart items
            List<CartItem> cartItemsOrder = new ArrayList<>();
            for (CartItem cartItem : cart.getCartItems()) {
                if ("InProgress".equals(cart.getStatus()) || cartItem.isChecked()) {
                    cartItemsOrder.add(cartItem);
                }
            }

            for (CartItem cartItem : cartItemsOrder) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItemRepository.save(orderItem);
                cartItemRepository.delete(cartItem);
            }

            // ✅ 9. Xóa cart nếu status là "Pending"
            if (cart.getStatus().equals("Pending")) {
                cartRepository.delete(cart);
            }

            // ✅ 10. Gửi thông báo
            notificationService.sendNotification(
                    user.getUserID(),
                    "Đơn hàng #" + order.getOrderID() + " đã được tạo thành công! Chờ xác nhận."
            );

            return ApiResponse.<Integer>builder()
                    .statusCode(200)
                    .message("Tạo đơn hàng COD thành công")
                    .data(order.getOrderID())
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

        boolean exists = paymentRepository.existsByTransactionCode(paymentCode);
        if (exists) return;

        Payment payment = new Payment();
        payment.setTransactionCode(paymentCode);
        payment.setAmount(amountStr != null ? Double.parseDouble(amountStr) / 100 : 0);
        payment.setPaymentDate(LocalDateTime.now());
        if ("00".equals(responseCode)) {
            // ✅ Giao dịch thành công
            Cart cart = cartRepository.findById(Integer.parseInt(cardIdStr))
                    .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
            List<CartItem> cartItemsOrder = new ArrayList<>();
            // For "InProgress" cart (ByCart), include all items. For other statuses, only include checked items
            for(CartItem cartItem : cart.getCartItems()){
                if ("InProgress".equals(cart.getStatus()) || cartItem.isChecked()){
                    cartItemsOrder.add(cartItem);
                }
            }
//            cart.setStatus("PAID");
//            cartRepository.save(cart);

            User user = cart.getUser();

            Order order = new Order();
            order.setUser(user);
            order.setTotalPrice(payment.getAmount());
            order.setBillingAddress(
                    (address != null && !address.isEmpty()) ? address : user.getAddress()
            );
            order.setOrderStatus(OrderStatus.PENDING);
            order.setOrderDate(LocalDateTime.now());
            order.setPaymentMethod(PaymentMethod.VNPAY);
            // Set delivery type - default to HOME_DELIVERY if no store info in address
            order.setDeliveryType(DeliveryType.HOME_DELIVERY);
            // Set shipping address from parameter, fallback to user address if not provided
            if (address != null && !address.isEmpty()) {
                order.setShippingAddress(address);
            } else if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                order.setShippingAddress(user.getAddress());
            }

            // Set recipient phone number
            if (recipientPhoneNumber != null && !recipientPhoneNumber.isEmpty()) {
                order.setRecipientPhoneNumber(recipientPhoneNumber);
            } else if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                order.setRecipientPhoneNumber(user.getPhoneNumber());
            }

            order = orderRepository.save(order);
            for (CartItem cartItem : cartItemsOrder) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItemRepository.save(orderItem);
                cartItemRepository.delete(cartItem);
            }
            if (cart.getStatus().equals("Pending")) {
                cartRepository.delete(cart);
            }


            payment.setPaymentStatus(true);
            payment.setOrder(order);
            paymentRepository.save(payment);

            // ✅ Gọi service gửi thông báo (tự động lưu DB + đẩy Firebase)
            notificationService.sendNotification(
                    user.getUserID(),
                    "Thanh toán thành công đơn hàng #" + order.getOrderID()
            );

            // ✅ Redirect theo client type
            String redirectUrl;
            if ("web".equalsIgnoreCase(clientType)) {
                redirectUrl = vnPayConfig.getWebBaseUrl() + "/payment-result?status=success&code=" + paymentCode + "&order=" + order.getOrderID();
            } else {
                redirectUrl = vnPayConfig.getAppDeepLink() + "/payment-result?status=success&code=" + paymentCode + "&order=" + order.getOrderID();
            }
            response.sendRedirect(redirectUrl);

        } else {
            // ❌ Giao dịch thất bại
            payment.setPaymentStatus(false);
            paymentRepository.save(payment);

            cartRepository.findById(Integer.parseInt(cardIdStr)).ifPresent(cart -> {
                User user = cart.getUser();
                if (user != null) {
                    // ✅ Gọi service gửi thông báo thất bại
                    notificationService.sendNotification(
                            user.getUserID(),
                            "Thanh toán thất bại. Mã giao dịch: " + paymentCode
                    );
                }
            });

            // ✅ Redirect theo client type
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

