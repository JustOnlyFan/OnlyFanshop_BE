package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import org.springframework.stereotype.Service;

public interface IProductService {
    public ApiResponse<HomepageResponse> getHomepage(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order);
}
