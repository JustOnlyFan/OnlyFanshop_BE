package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;

public interface IProductService {
    public ApiResponse<Object> getHomepage(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order);
    public ApiResponse<ProductDetailDTO> getProductDetail(Integer productId);
}
