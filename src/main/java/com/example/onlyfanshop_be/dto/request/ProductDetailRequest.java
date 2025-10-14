package com.example.onlyfanshop_be.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDetailRequest {
    private String productName;
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private Double price;
    private String imageURL;
    private Integer categoryID;
    private Integer brandID;
}
