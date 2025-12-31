package com.example.onlyfanshop_be.dto;

import lombok.*;
import com.example.onlyfanshop_be.entity.Color;
import com.example.onlyfanshop_be.entity.Warranty;
import java.math.BigDecimal;
import java.util.List;
import com.example.onlyfanshop_be.dto.ProductImageDTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailDTO {
    private Integer id;
    private String productName;
    private String slug;
    private String sku;
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private Double price;
    private String imageURL;
    private BrandDTO brand;
    private CategoryDTO category;
    
    // New fields from updated Product entity
    private Integer powerWatt; // Công suất (W)
    private BigDecimal bladeDiameterCm; // Đường kính cánh quạt (cm)
    private String colorDefault; // Legacy field - Màu sắc mặc định
    private Integer warrantyMonths; // Legacy field - Bảo hành (tháng)
    
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
    private List<Color> colors; // List of colors
    private Warranty warranty; // Warranty information
    
    // Quantity field
    private Integer quantity; // Số lượng sản phẩm
    
    // Multi-category support
    private List<ProductCategoryDTO> productCategories; // List of product categories
    private List<ProductTagDTO> productTags; // List of product tags
    private List<ProductImageDTO> images; // Danh sách ảnh (bao gồm ảnh theo màu)
}

