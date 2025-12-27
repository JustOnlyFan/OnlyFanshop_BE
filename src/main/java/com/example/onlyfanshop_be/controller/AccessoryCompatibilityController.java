package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.AccessoryCompatibilityDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.AccessoryCompatibility;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.service.AccessoryCompatibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/accessory-compatibility")
public class AccessoryCompatibilityController {

    @Autowired
    private AccessoryCompatibilityService accessoryCompatibilityService;

    @GetMapping("/public/product/{accessoryProductId}")
    public ResponseEntity<ApiResponse<List<AccessoryCompatibilityDTO>>> getCompatibilityByProduct(
            @PathVariable Long accessoryProductId) {
        try {
            List<AccessoryCompatibility> compatibilities = 
                    accessoryCompatibilityService.getCompatibilityByProductWithDetails(accessoryProductId);
            List<AccessoryCompatibilityDTO> dtos = compatibilities.stream()
                    .map(AccessoryCompatibilityDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                    .statusCode(200)
                    .message("Lấy thông tin tương thích thành công")
                    .data(dtos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/fan-type/{fanTypeId}/accessories")
    public ResponseEntity<ApiResponse<List<Product>>> getAccessoriesByFanType(@PathVariable Integer fanTypeId) {
        try {
            List<Product> accessories = accessoryCompatibilityService.getAccessoriesByFanType(fanTypeId);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy phụ kiện tương thích với loại quạt thành công")
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

    @GetMapping("/public/brand/{brandId}/accessories")
    public ResponseEntity<ApiResponse<List<Product>>> getAccessoriesByBrand(@PathVariable Integer brandId) {
        try {
            List<Product> accessories = accessoryCompatibilityService.getAccessoriesByBrand(brandId);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy phụ kiện tương thích với thương hiệu thành công")
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

    @GetMapping("/public/fan-type/{fanTypeId}/brand/{brandId}/accessories")
    public ResponseEntity<ApiResponse<List<Product>>> getAccessoriesByFanTypeAndBrand(
            @PathVariable Integer fanTypeId,
            @PathVariable Integer brandId) {
        try {
            List<Product> accessories = accessoryCompatibilityService.getAccessoriesByFanTypeAndBrand(fanTypeId, brandId);
            
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

    @GetMapping("/public/search")
    public ResponseEntity<ApiResponse<List<AccessoryCompatibilityDTO>>> searchByModel(
            @RequestParam String modelPattern) {
        try {
            List<AccessoryCompatibility> compatibilities = accessoryCompatibilityService.searchByModel(modelPattern);
            List<AccessoryCompatibilityDTO> dtos = compatibilities.stream()
                    .map(AccessoryCompatibilityDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                    .statusCode(200)
                    .message("Tìm kiếm tương thích theo model thành công")
                    .data(dtos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<AccessoryCompatibilityDTO>> getCompatibilityById(@PathVariable Long id) {
        try {
            AccessoryCompatibility compatibility = accessoryCompatibilityService.getCompatibilityById(id);
            AccessoryCompatibilityDTO dto = AccessoryCompatibilityDTO.fromEntity(compatibility);
            
            return ResponseEntity.ok(ApiResponse.<AccessoryCompatibilityDTO>builder()
                    .statusCode(200)
                    .message("Lấy thông tin tương thích thành công")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AccessoryCompatibilityDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<AccessoryCompatibilityDTO>> createCompatibility(
            @RequestBody AccessoryCompatibilityDTO dto) {
        try {
            AccessoryCompatibility compatibility = dto.toEntity();
            AccessoryCompatibility created = accessoryCompatibilityService.createCompatibility(compatibility);
            AccessoryCompatibilityDTO result = AccessoryCompatibilityDTO.fromEntity(created);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<AccessoryCompatibilityDTO>builder()
                            .statusCode(201)
                            .message("Tạo thông tin tương thích thành công")
                            .data(result)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AccessoryCompatibilityDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<AccessoryCompatibilityDTO>> updateCompatibility(
            @PathVariable Long id,
            @RequestBody AccessoryCompatibilityDTO dto) {
        try {
            AccessoryCompatibility compatibility = dto.toEntity();
            AccessoryCompatibility updated = accessoryCompatibilityService.updateCompatibility(id, compatibility);
            AccessoryCompatibilityDTO result = AccessoryCompatibilityDTO.fromEntity(updated);
            
            return ResponseEntity.ok(ApiResponse.<AccessoryCompatibilityDTO>builder()
                    .statusCode(200)
                    .message("Cập nhật thông tin tương thích thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<AccessoryCompatibilityDTO>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCompatibility(@PathVariable Long id) {
        try {
            accessoryCompatibilityService.deleteCompatibility(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Xóa thông tin tương thích thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/admin/product/{accessoryProductId}/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<AccessoryCompatibilityDTO>>> addCompatibilities(
            @PathVariable Long accessoryProductId,
            @RequestBody List<AccessoryCompatibilityDTO> dtos) {
        try {
            List<AccessoryCompatibility> compatibilities = dtos.stream()
                    .map(AccessoryCompatibilityDTO::toEntity)
                    .collect(Collectors.toList());
            
            List<AccessoryCompatibility> created = 
                    accessoryCompatibilityService.addCompatibilities(accessoryProductId, compatibilities);
            List<AccessoryCompatibilityDTO> results = created.stream()
                    .map(AccessoryCompatibilityDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                            .statusCode(201)
                            .message("Thêm thông tin tương thích thành công")
                            .data(results)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/admin/product/{accessoryProductId}/replace")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<AccessoryCompatibilityDTO>>> replaceCompatibilities(
            @PathVariable Long accessoryProductId,
            @RequestBody List<AccessoryCompatibilityDTO> dtos) {
        try {
            List<AccessoryCompatibility> compatibilities = dtos.stream()
                    .map(AccessoryCompatibilityDTO::toEntity)
                    .collect(Collectors.toList());
            
            List<AccessoryCompatibility> created = 
                    accessoryCompatibilityService.replaceCompatibilities(accessoryProductId, compatibilities);
            List<AccessoryCompatibilityDTO> results = created.stream()
                    .map(AccessoryCompatibilityDTO::fromEntity)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                    .statusCode(200)
                    .message("Cập nhật thông tin tương thích thành công")
                    .data(results)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<AccessoryCompatibilityDTO>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/admin/product/{accessoryProductId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAllByAccessoryProduct(@PathVariable Long accessoryProductId) {
        try {
            accessoryCompatibilityService.deleteAllByAccessoryProductId(accessoryProductId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .statusCode(200)
                    .message("Xóa tất cả thông tin tương thích thành công")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/admin/product/{accessoryProductId}/has-entries")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Boolean>> hasCompatibilityEntries(@PathVariable Long accessoryProductId) {
        try {
            boolean hasEntries = accessoryCompatibilityService.hasCompatibilityEntries(accessoryProductId);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .statusCode(200)
                    .message(hasEntries ? "Phụ kiện có thông tin tương thích" : "Phụ kiện chưa có thông tin tương thích")
                    .data(hasEntries)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Boolean>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/admin/product/{accessoryProductId}/count")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Long>> getCompatibilityCount(@PathVariable Long accessoryProductId) {
        try {
            long count = accessoryCompatibilityService.getCompatibilityCount(accessoryProductId);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .statusCode(200)
                    .message("Lấy số lượng thông tin tương thích thành công")
                    .data(count)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Long>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }
}
