package com.example.onlyfanshop_be.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BrandDTO {
    private Integer brandID;
    private String name;
    private String country;
    private String description;
    private String imageURL;
    private boolean isActive;
}
