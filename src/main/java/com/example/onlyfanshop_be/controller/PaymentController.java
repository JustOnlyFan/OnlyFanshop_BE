package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Payment;
import com.example.onlyfanshop_be.repository.PaymentRepository;
import com.example.onlyfanshop_be.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @Autowired
    private final PaymentRepository paymentRepository;
    @GetMapping("/vn-pay")
    public ApiResponse<PaymentDTO.VNPayResponse> pay(HttpServletRequest request, @RequestParam Double amount, @RequestParam String bankCode) {
        return ApiResponse.<PaymentDTO.VNPayResponse>builder().statusCode(200).message("Thanh cong").data(paymentService.createVnPayPayment(request)).build();
    }
    @GetMapping("/vn-pay-callback")
    public void vnPayCallback(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String responseCode = params.get("vnp_ResponseCode");
        String paymentCode = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");

        // Kiểm tra transactionCode đã tồn tại chưa (tránh duplicate)
        boolean exists = paymentRepository.existsByTransactionCode(paymentCode);
        if (exists) {

            return;
        }

        Payment payment = new Payment();
        payment.setTransactionCode(paymentCode);
        payment.setAmount(amountStr != null ? Double.parseDouble(amountStr) / 100 : 0); // VNPay gửi amount * 100
        payment.setPaymentDate(LocalDateTime.now());

        if ("00".equals(responseCode)) {
            // Giao dịch thành công
            payment.setPaymentStatus(true);
            paymentRepository.save(payment);
        } else {
            // Giao dịch thất bại
            payment.setPaymentStatus(false);
            paymentRepository.save(payment);

        }
    }

}

