package com.example.onlyfanshop_be.dto;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryManagementDTO {
    private String categoryID;
    private String categoryName;
    private boolean isActive;
}
