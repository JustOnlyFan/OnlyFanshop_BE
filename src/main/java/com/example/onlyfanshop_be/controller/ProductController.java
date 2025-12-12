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
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private IProductService iProductService;

    @PostMapping("/public/homepage")
    public ResponseEntity<ApiResponse<HomepageResponse>> getHomepage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer bladeCount,
            @RequestParam(required = false) Boolean remoteControl,
            @RequestParam(required = false) Boolean oscillation,
            @RequestParam(required = false) Boolean timer,
            @RequestParam(required = false) Integer minPower,
            @RequestParam(required = false) Integer maxPower,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {
        try {
            System.out.println("Homepage request - sortBy: " + sortBy + ", order: " + order);
            ApiResponse<HomepageResponse> response = iProductService.getHomepage(
                    keyword, categoryId, brandId, minPrice, maxPrice, bladeCount,
                    remoteControl, oscillation, timer, minPower, maxPower,
                    page, size, sortBy, order);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        } catch (Exception e) {
            System.err.println("Error in getHomepage: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<HomepageResponse>builder()
                            .statusCode(400)
                            .message("Lỗi khi lấy trang chủ: " + e.getMessage())
                            .build());
        }
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
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> createProduct(@RequestBody ProductDetailRequest product) {
        try {
            // Debug: Check current authentication
            org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                System.out.println("ProductController.createProduct: Authenticated user: " + auth.getName());
                System.out.println("ProductController.createProduct: Authorities: " + auth.getAuthorities());
            } else {
                System.out.println("ProductController.createProduct: No authentication found!");
            }
            
            System.out.println("Creating product with data: " + product);
            System.out.println("Product name: " + product.getProductName());
            System.out.println("Brand ID: " + product.getBrandID());
            System.out.println("Category ID: " + product.getCategoryID());
            
            // Validate required fields
            if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.builder()
                                .statusCode(400)
                                .message("Tên sản phẩm không được để trống")
                                .build());
            }
            
            if (product.getBrandID() == null || product.getBrandID() == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.builder()
                                .statusCode(400)
                                .message("Vui lòng chọn thương hiệu")
                                .build());
            }
            
            Product createdProduct = iProductService.createProduct(product);
            System.out.println("Product created successfully with ID: " + createdProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
        } catch (RuntimeException e) {
            System.err.println("Error creating product: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            System.err.println("Unexpected error creating product: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .statusCode(500)
                            .message("Lỗi không xác định: " + e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody ProductDetailRequest product) {
        try {
            ProductDetailDTO updatedProduct = iProductService.updateProduct(id, product);
            return ResponseEntity.ok(updatedProduct);
        } catch (RuntimeException e) {
            System.err.println("Error updating product: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            System.err.println("Unexpected error updating product: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .statusCode(500)
                            .message("Lỗi không xác định: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProduct(@PathVariable Integer id) {
        iProductService.deleteProduct(id);
        return "Xóa sản phẩm có ID " + id + " thành công!";
    }

    @PutMapping("/image/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Void> updateImage(@PathVariable Integer id, @RequestParam String imgString) {
         iProductService.updateImage(id, imgString);
        return ApiResponse.<Void>builder().message("Cập nhật thành công").statusCode(200).build();
    }
    @PostMapping("/productList")
    public ResponseEntity<ApiResponse<HomepageResponse>> productList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Integer bladeCount,
            @RequestParam(required = false) Boolean remoteControl,
            @RequestParam(required = false) Boolean oscillation,
            @RequestParam(required = false) Boolean timer,
            @RequestParam(required = false) Integer minPower,
            @RequestParam(required = false) Integer maxPower,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {
        try {
            System.out.println("ProductList request - sortBy: " + sortBy + ", order: " + order);
            ApiResponse<HomepageResponse> response = iProductService.productList(
                    keyword, categoryId, brandId, minPrice, maxPrice, bladeCount,
                    remoteControl, oscillation, timer, minPower, maxPower,
                    page, size, sortBy, order);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(response);
        } catch (Exception e) {
            System.err.println("Error in productList: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<HomepageResponse>builder()
                            .statusCode(400)
                            .message("Lỗi khi lấy danh sách sản phẩm: " + e.getMessage())
                            .build());
        }
    }
    @PutMapping("/active/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<Void> updateActive(@PathVariable Integer id, @RequestParam boolean active) {
        iProductService.updateActive(id,active );
        return ApiResponse.<Void>builder().message("Cập nhật thành công").statusCode(200).build();
    }
}