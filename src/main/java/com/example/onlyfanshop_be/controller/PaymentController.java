package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.*;
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
    //    @GetMapping("/vn-pay")
//    public ApiResponse<PaymentDTO.VNPayResponse> pay(HttpServletRequest request, @RequestParam Double amount, @RequestParam String bankCode,@RequestParam int cardId) {
//        return ApiResponse.<PaymentDTO.VNPayResponse>builder().statusCode(200).message("Thanh cong").data(paymentService.createVnPayPayment(request,amount,bankCode, cardId)).build();
//    }
    @GetMapping("/vn-pay")
    public ApiResponse<PaymentDTO.VNPayResponse> pay(
            HttpServletRequest request,
            @RequestParam Double amount,
            @RequestParam String bankCode,
            @RequestParam String address
    ) {
        // ✅ 1. Lấy token từ header
        String token = jwtTokenProvider.extractToken(request);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);

        // ✅ 3. Lấy cart tương ứng với user
        Cart cart= cartRepository.findByUser_UserIDAndStatus(userid, "InstantBuy").orElse(null);
        if (cart == null) {
            cart = cartRepository.findByUser_UserIDAndStatus(userid, "InProgress")
                    .orElseThrow(() -> new AppException(ErrorCode.CART_NOTFOUND));
        }else {
            cart.setStatus("InstantBuy*");
            cartRepository.save(cart);}

        // ✅ 4. Gọi service xử lý thanh toán
        PaymentDTO.VNPayResponse responseData = paymentService.createVnPayPayment(request, amount, bankCode, cart.getCartID(),address);

        return ApiResponse.<PaymentDTO.VNPayResponse>builder()
                .statusCode(200)
                .message("Tạo thanh toán thành công")
                .data(responseData)
                .build();
    }

    @GetMapping("/public/vn-pay-callback")
    public void vnPayCallback(@RequestParam Map<String, String> params,
                              @RequestParam(required = false) String address,
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
            for(CartItem cartItem : cart.getCartItems()){
                if (cartItem.isChecked()){
                    cartItemsOrder.add(cartItem);
                }
            }
//            cart.setStatus("PAID");
//            cartRepository.save(cart);

            User user = cart.getUser();

            Order order = new Order();
            order.setUser(user);
            order.setTotalPrice(payment.getAmount());
            //order.setCart(cart);
            order.setBillingAddress(
                    (address != null && !address.isEmpty()) ? address : user.getAddress()
            );
            order.setOrderStatus("CONFIRMED");
            order.setOrderDate(LocalDateTime.now());
            order.setPaymentMethod("VNPay");

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
            if (cart.getStatus().equals("InstantBuy*")) {
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

            response.sendRedirect("https://onlyfanshop.app/payment-result?status=success&code="
                    + paymentCode + "&order=" + order.getOrderID());

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

            response.sendRedirect("https://onlyfanshop.app/payment-result?status=fail&code=" + paymentCode);
        }
    }


}

