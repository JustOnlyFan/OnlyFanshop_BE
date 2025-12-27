package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.request.ProductFilterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.service.ProductFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products/filter")
public class ProductFilterController {

    @Autowired
    private ProductFilterService productFilterService;

    @PostMapping("/public")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filterProducts(
            @RequestBody ProductFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            // Build pageable with sorting
            Sort sort = sortDirection.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Apply filters
            Page<Product> productPage = productFilterService.filterProducts(request, pageable);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("products", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalItems", productPage.getTotalElements());
            response.put("totalPages", productPage.getTotalPages());
            response.put("pageSize", productPage.getSize());
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .statusCode(200)
                    .message("Lọc sản phẩm thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "true") boolean includeSubcategories) {
        try {
            List<Product> products = productFilterService.getProductsByCategory(categoryId, includeSubcategories);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy sản phẩm theo danh mục thành công")
                    .data(products)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/price-range")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByPriceRange(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        try {
            List<Product> products = productFilterService.getProductsByPriceRange(minPrice, maxPrice);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy sản phẩm theo khoảng giá thành công")
                    .data(products)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/accessories/compatible/{fanTypeId}")
    public ResponseEntity<ApiResponse<List<Product>>> getAccessoriesByCompatibleFanType(
            @PathVariable Integer fanTypeId) {
        try {
            List<Product> accessories = productFilterService.getAccessoriesByCompatibleFanType(fanTypeId);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy phụ kiện tương thích thành công")
                    .data(accessories)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/category/{categoryId}/descendants")
    public ResponseEntity<ApiResponse<List<Integer>>> getCategoryDescendants(@PathVariable Integer categoryId) {
        try {
            List<Integer> categoryIds = productFilterService.getAllCategoryIdsIncludingDescendants(categoryId);
            
            return ResponseEntity.ok(ApiResponse.<List<Integer>>builder()
                    .statusCode(200)
                    .message("Lấy danh sách ID danh mục con thành công")
                    .data(categoryIds)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Integer>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }
}
