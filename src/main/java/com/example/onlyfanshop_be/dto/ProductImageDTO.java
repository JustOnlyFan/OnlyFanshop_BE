package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Color;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {
    private Long id;
    private Long productId;
    private String imageUrl;
    private Boolean isMain;
    private Integer sortOrder;
    private Integer colorId;
    private Color color;
}
