package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.enums.ShippingCarrier;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShipmentRequest {
    private ShipmentType shipmentType;
    private Long orderId;
    private Long inventoryRequestId;
    private ShippingCarrier carrier;
    
    // Người gửi (nếu không có sẽ lấy từ store)
    private String fromName;
    private String fromPhone;
    private String fromAddress;
    private String fromWardCode;
    private Integer fromDistrictId;
    private Integer fromStoreId;
    
    // Người nhận
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;
    private Integer toDistrictId;
    
    // Thông tin hàng
    private Integer weight; // gram
    private Integer length; // cm
    private Integer width;  // cm
    private Integer height; // cm
    
    // Giá trị
    private BigDecimal codAmount;
    private BigDecimal insuranceValue;
    
    // Dịch vụ GHN
    private Integer serviceTypeId; // 2: Standard, 5: Express
    private Integer paymentTypeId; // 1: Shop trả, 2: Khách trả
    private String note;
    private String requiredNote; // CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG
    
    // Danh sách sản phẩm (cho GHN)
    private List<ShipmentItem> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShipmentItem {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer weight;
    }
}
