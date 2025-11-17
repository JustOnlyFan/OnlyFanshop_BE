package com.example.onlyfanshop_be.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductDetailRequest {
    private String productName;
    // SKU and Slug will be auto-generated, not accepted from request
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private Double price;
    private String imageURL;
    private Integer categoryID;
    private Integer brandID;
    
    // New fields from updated Product entity
    private Integer powerWatt; // Công suất (W)
    private BigDecimal bladeDiameterCm; // Đường kính cánh quạt (cm)
    private String colorDefault; // Legacy field, keep for backward compatibility
    private Integer warrantyMonths; // Legacy field, keep for backward compatibility
    
    // Technical specifications
    private String voltage; // Điện áp sử dụng: "220V / 50Hz"
    private String windSpeedLevels; // Tốc độ gió: "3 mức" hoặc "Điều chỉnh vô cấp"
    private Integer airflow; // Lưu lượng gió: m³/phút
    private String bladeMaterial; // Chất liệu cánh quạt: "Nhựa ABS" / "Kim loại"
    private String bodyMaterial; // Chất liệu thân quạt: "Nhựa cao cấp" / "Thép sơn tĩnh điện"
    private Integer bladeCount; // Số lượng cánh: 3 / 5
    private Integer noiseLevel; // Mức độ ồn: dB
    private Integer motorSpeed; // Tốc độ quay motor: vòng/phút
    private BigDecimal weight; // Trọng lượng: kg
    private String adjustableHeight; // Chiều cao điều chỉnh: "1.1 – 1.4 m"
    
    // Features
    private Boolean remoteControl; // Điều khiển từ xa
    private String timer; // Hẹn giờ tắt: "1 – 4 giờ"
    private Boolean naturalWindMode; // Chế độ gió tự nhiên
    private Boolean sleepMode; // Chế độ ngủ
    private Boolean oscillation; // Đảo chiều gió
    private Boolean heightAdjustable; // Điều chỉnh độ cao
    private Boolean autoShutoff; // Ngắt điện tự động khi quá tải
    private Boolean temperatureSensor; // Cảm biến nhiệt
    private Boolean energySaving; // Tiết kiệm điện
    
    // Other information
    private String safetyStandards; // Tiêu chuẩn an toàn: "TCVN / IEC / RoHS"
    private Integer manufacturingYear; // Năm sản xuất: 2025
    private String accessories; // Phụ kiện đi kèm: "Điều khiển / Pin / HDSD"
    private String energyRating; // Mức tiết kiệm điện năng: "5 sao"
    
    // New relationship fields
    private List<Integer> colorIds; // List of color IDs
    private Integer warrantyId; // Warranty ID
    
    // Quantity field
    private Integer quantity; // Số lượng sản phẩm
    
}
