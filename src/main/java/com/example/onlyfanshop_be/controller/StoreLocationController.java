package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.dto.request.StoreLocationRequest;
import com.example.onlyfanshop_be.dto.request.CreateStaffRequest;
import com.example.onlyfanshop_be.enums.StoreStatus;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.service.IStoreLocation;
import com.example.onlyfanshop_be.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/store-locations")
@RequiredArgsConstructor
@Slf4j
public class StoreLocationController {

    private final IStoreLocation iStoreLocation;
    private final StaffService staffService;

    // 🟢 Lấy tất cả địa điểm
    @GetMapping
    public ApiResponse<List<StoreLocation>> getAll() {
        return ApiResponse.<List<StoreLocation>>builder()
                .statusCode(200)
                .message("Danh sách địa điểm cửa hàng")
                .data(iStoreLocation.getAllLocations())
                .build();
    }

    // 🟢 Lấy 1 địa điểm theo ID
    @GetMapping("/{id}")
    public ApiResponse<StoreLocation> getById(@PathVariable Integer id) {
        return ApiResponse.<StoreLocation>builder()
                .statusCode(200)
                .message("Chi tiết địa điểm")
                .data(iStoreLocation.getLocationById(id))
                .build();
    }

    // 🟢 Thêm mới
    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ApiResponse<StoreLocation> create(@Valid @RequestBody StoreLocationRequest request) {
        // Map request to entity, support both phone and phoneNumber, and take first image if provided
        String resolvedPhone = request.getPhone() != null ? request.getPhone() : request.getPhoneNumber();
        String resolvedImage = request.getImageUrl();
        if ((resolvedImage == null || resolvedImage.isBlank()) && request.getImages() != null && !request.getImages().isEmpty()) {
            resolvedImage = request.getImages().get(0);
        }

        StoreLocation location = StoreLocation.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(resolvedImage)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .ward(request.getWard())
                .city(request.getCity())
                .phone(resolvedPhone)
                .email(request.getEmail())
                .openingHours(request.getOpeningHours())
                .status(request.getStatus() != null ? request.getStatus() : StoreStatus.ACTIVE)
                .build();
        
        // Use the new method that creates store with staff and warehouse automatically
        // Requirements: 3.2, 3.3, 3.4
        StoreLocation saved = iStoreLocation.createStoreWithStaffAndWarehouse(location, request.getStaffPassword());

        return ApiResponse.<StoreLocation>builder()
                .statusCode(201)
                .message("Tạo địa điểm thành công")
                .data(saved)
                .build();
    }

    // 🟢 Cập nhật
    @PutMapping("/{id}")
    public ApiResponse<StoreLocation> update(@PathVariable Integer id, @Valid @RequestBody StoreLocationRequest request) {
        StoreLocation location = StoreLocation.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .openingHours(request.getOpeningHours())
                .status(request.getStatus())
                .build();
        return ApiResponse.<StoreLocation>builder()
                .statusCode(200)
                .message("Cập nhật địa điểm thành công")
                .data(iStoreLocation.updateLocation(id, location))
                .build();
    }

    // 🟢 Xóa
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        iStoreLocation.deleteLocation(id);
        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Xóa địa điểm thành công")
                .build();
    }

    // 🟢 Tạo staff account cho store (nếu chưa có) - dùng để tạo lại cho các store đã tạo trước đó
    @PostMapping("/{id}/create-staff")
    public ApiResponse<com.example.onlyfanshop_be.dto.StaffDTO> createStaffForStore(
            @PathVariable Integer id,
            @RequestBody(required = false) java.util.Map<String, String> request) {
        try {
            // Verify store exists
            iStoreLocation.getLocationById(id);
            
            // Check if store already has staff
            var existingStaff = staffService.getStaffByStoreLocation(id);
            if (!existingStaff.isEmpty()) {
                return ApiResponse.<com.example.onlyfanshop_be.dto.StaffDTO>builder()
                        .statusCode(400)
                        .message("Store already has staff account")
                        .data(existingStaff.get(0))
                        .build();
            }
            
            CreateStaffRequest staffRequest = new CreateStaffRequest();
            staffRequest.setStoreLocationId(id);
            String password = (request != null && request.containsKey("password")) 
                    ? request.get("password") 
                    : "Staff@123";
            staffRequest.setPassword(password);
            
            var staffDTO = staffService.createStaff(staffRequest);
            log.info("Successfully created staff account with ID: {} for store ID: {}", 
                    staffDTO.getUserID(), id);
            
            return ApiResponse.<com.example.onlyfanshop_be.dto.StaffDTO>builder()
                    .statusCode(201)
                    .message("Tạo tài khoản nhân viên thành công")
                    .data(staffDTO)
                    .build();
        } catch (Exception e) {
            log.error("Failed to create staff for store ID: {} - Error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // 🟢 Lấy danh sách cửa hàng có sản phẩm trong kho
    @GetMapping("/product/{productId}")
    public ApiResponse<List<StoreLocation>> getStoresWithProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district) {
        log.info("Getting stores with product ID: {}, city: {}, district: {}", productId, city, district);
        List<StoreLocation> stores = iStoreLocation.getStoresWithProduct(productId, city, district);
        log.info("Returning {} stores for product {}", stores.size(), productId);
        return ApiResponse.<List<StoreLocation>>builder()
                .statusCode(200)
                .message("Danh sách cửa hàng có sản phẩm")
                .data(stores)
                .build();
    }
}

