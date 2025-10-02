package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.IProductService;
import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private IProductService iProductService;
    @PostMapping("/homepage")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ProductID") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {

        ApiResponse<Object> response = iProductService.getHomepage(keyword, categoryId, brandId, page, size, sortBy, order);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductDetail(@PathVariable("id") Integer id) {
        ApiResponse<ProductDetailDTO> response = iProductService.getProductDetail(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
