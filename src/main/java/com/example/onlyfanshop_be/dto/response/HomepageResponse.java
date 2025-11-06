package com.example.onlyfanshop_be.dto.response;

import com.example.onlyfanshop_be.dto.BrandDTO;
import com.example.onlyfanshop_be.dto.CategoryDTO;
import com.example.onlyfanshop_be.dto.Pagination;
import com.example.onlyfanshop_be.dto.ProductDTO;
import lombok.*;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HomepageResponse {
    private Filters filters;
    private List<CategoryDTO> categories;
    private List<BrandDTO> brands;
    private List<ProductDTO> products;
    private Pagination pagination;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Filters {
        private String selectedCategory;
        private String selectedBrand;
        private String sortOption;
        private Long maxPrice;
        private Long minPrice;
    }
}
