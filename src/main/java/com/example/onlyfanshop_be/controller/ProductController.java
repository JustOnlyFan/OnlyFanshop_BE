package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.request.ProductDetailRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import com.example.onlyfanshop_be.dto.response.HomepageResponse;
import com.example.onlyfanshop_be.service.IProductService;
import com.example.onlyfanshop_be.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private IProductService iProductService;

    @PostMapping("/public/homepage")
    public ResponseEntity<ApiResponse<HomepageResponse>> resetPassword(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ProductID") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {

        ApiResponse<HomepageResponse> response = iProductService.getHomepage(keyword, categoryId, brandId, page, size, sortBy, order);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/public/detail/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductDetail(@PathVariable Integer productId) {
        System.out.println("Getting product detail for ID: " + productId);
        ApiResponse<ProductDetailDTO> response = iProductService.getProductDetail(productId);
        System.out.println("Response: " + response);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
    @GetMapping
    public List<Product> getAllProducts() {
        return iProductService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductDetailDTO getProductById(@PathVariable Integer id) {
        return iProductService.getProductById(id);
    }

    @PostMapping
    public Product createProduct(@RequestBody ProductDetailRequest product) {
        return iProductService.createProduct(product);
    }

    @PutMapping("/{id}")
    public ProductDetailDTO updateProduct(@PathVariable Integer id, @RequestBody ProductDetailRequest product) {
        return iProductService.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Integer id) {
        iProductService.deleteProduct(id);
        return "Xóa sản phẩm có ID " + id + " thành công!";
    }
}