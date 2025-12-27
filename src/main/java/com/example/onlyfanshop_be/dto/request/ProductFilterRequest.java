package com.example.onlyfanshop_be.dto.request;

import com.example.onlyfanshop_be.enums.CategoryType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {

    private List<Integer> categoryIds;

    private List<CategoryType> categoryTypes;

    private List<Integer> brandIds;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private List<String> tagCodes;

    private Integer compatibleFanTypeId;

    private String searchQuery;

    private String sortBy;

    private String sortDirection;

    @Builder.Default
    private Boolean includeSubcategories = true;
}
