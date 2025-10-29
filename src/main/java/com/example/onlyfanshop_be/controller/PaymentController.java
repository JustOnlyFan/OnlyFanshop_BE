package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.CartRepository;
import com.example.onlyfanshop_be.repository.NotificationRepository;
import com.example.onlyfanshop_be.repository.OrderRepository;
import com.example.onlyfanshop_be.repository.PaymentRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
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
        String cardIdStr = params.get("vnp_TxnRef");

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

            cart.setStatus("PAID");
            cartRepository.save(cart);

            User user = cart.getUser();

            Order order = new Order();
            order.setUser(user);
            order.setCart(cart);
            order.setBillingAddress(
                    (address != null && !address.isEmpty()) ? address : user.getAddress()
            );
            order.setOrderStatus("confirmed");
            order.setOrderDate(LocalDateTime.now());
            order.setPaymentMethod("VNPay");

            order = orderRepository.save(order);

            payment.setPaymentStatus(true);
            payment.setOrder(order);
            paymentRepository.save(payment);

            // ✅ Tạo thông báo khi thanh toán thành công
            Notification notification = Notification.builder()
                    .user(user)
                    .message("Thanh toán thành công đơn hàng #" + order.getOrderID())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);

            response.sendRedirect("https://onlyfanshop.app/payment-result?status=success&code="
                    + paymentCode + "&order=" + order.getOrderID());
        } else {
            // ❌ Giao dịch thất bại
            payment.setPaymentStatus(false);
            paymentRepository.save(payment);

            // ✅ Tạo thông báo khi thanh toán thất bại
            Notification notification = Notification.builder()
                    .user(cartRepository.findById(Integer.parseInt(cardIdStr))
                            .map(Cart::getUser)
                            .orElse(null))
                    .message("Thanh toán thất bại. Mã giao dịch: " + paymentCode)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            if (notification.getUser() != null) {
                notificationRepository.save(notification);
            }

            response.sendRedirect("https://onlyfanshop.app/payment-result?status=fail&code=" + paymentCode);
        }
    }

}

