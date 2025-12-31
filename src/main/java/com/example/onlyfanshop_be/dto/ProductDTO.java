package com.example.onlyfanshop_be.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO {
    private Integer id;
    private String productName;
    private Double price;
    private String imageURL;
    private String briefDescription;
    private BrandDTO brand;
    private CategoryDTO category;
    private boolean isActive;
    private List<ProductImageDTO> images;
}
