package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.ShipmentStatus;
import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.enums.ShippingCarrier;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDTO {
    private Long id;
    private ShipmentType shipmentType;
    private Long orderId;
    private Long inventoryRequestId;
    private ShippingCarrier carrier;
    private String trackingNumber;
    private String carrierOrderCode;
    private ShipmentStatus status;
    
    // Người gửi
    private String fromName;
    private String fromPhone;
    private String fromAddress;
    private String fromWardCode;
    private Integer fromDistrictId;
    
    // Người nhận
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;
    private Integer toDistrictId;
    
    // Thông tin hàng
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    
    // Giá trị
    private BigDecimal codAmount;
    private BigDecimal insuranceValue;
    private BigDecimal shippingFee;
    
    // Dịch vụ
    private Integer serviceTypeId;
    private Integer paymentTypeId;
    private String note;
    private String requiredNote;
    
    // Thời gian
    private LocalDateTime expectedDeliveryTime;
    private LocalDateTime pickedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    
    private Integer fromStoreId;
}
