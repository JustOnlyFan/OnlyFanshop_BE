package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.entity.Product;

import java.util.List;

public interface IProductService {
    public ApiResponse<HomepageResponse> getHomepage(String keyword, Integer categoryId, Integer brandId, int page, int size, String sortBy, String order);
    public ApiResponse<ProductDetailDTO> getProductDetail(Integer productId);
    public List<Product> getAllProducts();
    public Product getProductById(int id);
    public Product createProduct(Product product);
    public Product updateProduct(Integer id, Product updatedProduct);
    public void deleteProduct(int id);
}
