package com.example.onlyfanshop_be.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailsDTO {

    private Integer orderID;          // Mã đơn hàng
    private String paymentMethod;     // Phương thức thanh toán (VNPay, COD,...)
    private String billingAddress;    // Địa chỉ thanh toán
    private String orderStatus;       // Trạng thái đơn hàng
    private LocalDateTime orderDate;  // Ngày đặt hàng
    private Double totalPrice;        // Tổng tiền đơn hàng

    private String address;           // Địa chỉ giao hàng
    private String customerName;      // Tên khách hàng
    private String email;             // Email khách hàng
    private String phone;

    private CartDTO cartDTO;          // Nếu bạn muốn lấy toàn bộ giỏ hàng
    private java.util.List<OrderItemLiteDTO> itemsLite; // Danh sách nhẹ để client dễ dùng
}
