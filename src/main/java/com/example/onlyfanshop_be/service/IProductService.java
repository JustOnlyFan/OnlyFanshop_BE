package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.ProductDetailRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.entity.Product;

import java.util.List;

public interface IProductService {
    public ApiResponse<HomepageResponse> getHomepage(
            String keyword, Integer categoryId, Integer brandId,
            Long minPrice, Long maxPrice, Integer bladeCount,
            Boolean remoteControl, Boolean oscillation, Boolean timer,
            Integer minPower, Integer maxPower,
            int page, int size, String sortBy, String order);
    public ApiResponse<ProductDetailDTO> getProductDetail(Integer productId);
    public List<Product> getAllProducts();
    public ProductDetailDTO getProductById(int id);
    public Product createProduct(ProductDetailRequest product);
    public ProductDetailDTO updateProduct(Integer id, ProductDetailRequest updatedProduct);
    public void deleteProduct(int id);
    public void updateImage(int productId, String imageURL);
    public ApiResponse<HomepageResponse> productList(
            String keyword, Integer categoryId, Integer brandId,
            Long minPrice, Long maxPrice, Integer bladeCount,
            Boolean remoteControl, Boolean oscillation, Boolean timer,
            Integer minPower, Integer maxPower,
            int page, int size, String sortBy, String order);
    public void updateActive(int productId, boolean active);
    public void updateActiveByBrand(int brandID);
    public void updateActiveByCategory(int categoryID);
}