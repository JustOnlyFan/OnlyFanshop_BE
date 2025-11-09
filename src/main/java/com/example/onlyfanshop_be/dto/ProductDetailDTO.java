package com.example.onlyfanshop_be.dto;

import lombok.*;
import com.example.onlyfanshop_be.entity.Color;
import com.example.onlyfanshop_be.entity.Warranty;

import java.math.BigDecimal;
import java.util.List;

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
    
    // New relationship fields
    private List<Color> colors; // List of colors
    private Warranty warranty; // Warranty information
}



