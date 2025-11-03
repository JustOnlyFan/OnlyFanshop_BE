package com.example.onlyfanshop_be.config;


import com.example.onlyfanshop_be.ultils.VNPayUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Configuration
public class VNPAYConfig {
    @Getter
    @Value("${PAY_URL}")
    private String vnp_PayUrl;
    @Value("${RETURN_URL}")
    private String vnp_ReturnUrl;
    @Getter
    @Value("${WEB_BASE_URL:http://localhost:3000}")
    private String webBaseUrl;
    @Getter
    @Value("${APP_DEEP_LINK:https://onlyfanshop.app}")
    private String appDeepLink;
    @Value("${TMN_CODE}")
    private String vnp_TmnCode;
    @Getter
    @Value("${SECRET_KEY}")
    private String secretKey;
    @Value("${VERSION}")
    private String vnp_Version;
    @Value("${COMMAND}")
    private String vnp_Command;
    @Value("${ORDER_TYPE}")
    private String orderType;

    public Map<String, String> getVNPayConfig(int cartId, String address, String recipientPhoneNumber, String clientType) {
        Map<String, String> vnpParamsMap = new HashMap<>();
        vnpParamsMap.put("vnp_Version", this.vnp_Version);
        vnpParamsMap.put("vnp_Command", this.vnp_Command);
        vnpParamsMap.put("vnp_TmnCode", this.vnp_TmnCode);
        vnpParamsMap.put("vnp_CurrCode", "VND");
        vnpParamsMap.put("vnp_TxnRef",  String.valueOf(cartId)+"_"+VNPayUtil.getRandomNumber(8));
        vnpParamsMap.put("vnp_OrderInfo", "Thanh toan don hang:" + VNPayUtil.getRandomNumber(8));
        vnpParamsMap.put("vnp_OrderType", this.orderType);
        vnpParamsMap.put("vnp_Locale", "vn");
        String encodedAddress = URLEncoder.encode(address != null ? address : "", StandardCharsets.UTF_8);
        String encodedPhone = URLEncoder.encode(recipientPhoneNumber != null ? recipientPhoneNumber : "", StandardCharsets.UTF_8);
        String encodedClientType = URLEncoder.encode(clientType != null ? clientType : "web", StandardCharsets.UTF_8);
        String returnUrl = vnp_ReturnUrl + "?address=" + encodedAddress + "&clientType=" + encodedClientType;
        if (recipientPhoneNumber != null && !recipientPhoneNumber.isEmpty()) {
            returnUrl += "&recipientPhoneNumber=" + encodedPhone;
        }
        vnpParamsMap.put("vnp_ReturnUrl", returnUrl);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParamsMap.put("vnp_ExpireDate", vnp_ExpireDate);
        return vnpParamsMap;
    }
}

