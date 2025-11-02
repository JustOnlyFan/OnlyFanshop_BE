package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.PaymentDTO;
import com.example.onlyfanshop_be.config.VNPAYConfig;
import com.example.onlyfanshop_be.ultils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final VNPAYConfig vnPayConfig;
    public PaymentDTO.VNPayResponse createVnPayPayment(HttpServletRequest request, Double amount, String bankCode, int cartId, String address, String recipientPhoneNumber) {
        long amountValue = Math.round(amount * 100); // nhân 100 theo quy định VNPay
        Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig(cartId, address, recipientPhoneNumber);

        vnpParamsMap.put("vnp_Amount", String.valueOf(amountValue));

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParamsMap.put("vnp_BankCode", bankCode);
        }

        vnpParamsMap.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        String paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
        System.out.println("paymentUrl: " + paymentUrl);
        return PaymentDTO.VNPayResponse.builder()
                .code("ok")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();
    }

}
