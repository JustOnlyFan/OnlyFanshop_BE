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
    
    // New relationship fields
    private List<Integer> colorIds; // List of color IDs
    private Integer warrantyId; // Warranty ID
}
