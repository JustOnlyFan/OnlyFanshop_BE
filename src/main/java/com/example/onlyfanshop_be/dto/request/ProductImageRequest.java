package com.example.onlyfanshop_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {
    private Integer colorId;
    private String imageUrl;
    private Boolean isMain;
    private Integer sortOrder;
}
