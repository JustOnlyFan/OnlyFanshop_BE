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

    // ✅ Thông tin người mua
    private String address;           // Địa chỉ giao hàng
    private String customerName;      // Tên khách hàng
    private String email;             // Email khách hàng
    private String phone;             // ☑️ Gợi ý thêm: số điện thoại người nhận

    // ✅ Giỏ hàng hoặc danh sách sản phẩm
    private CartDTO cartDTO;          // Nếu bạn muốn lấy toàn bộ giỏ hàng
}
