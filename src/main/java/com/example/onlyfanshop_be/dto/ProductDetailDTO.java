package com.example.onlyfanshop_be.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailDTO {
    private Integer id;
    private String productName;
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private Double price;
    private String imageURL;
    private BrandDTO brand;
    private CategoryDTO category;
}


